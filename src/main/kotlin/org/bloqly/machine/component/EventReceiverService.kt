package org.bloqly.machine.component

import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.block.BlockData
import org.bloqly.machine.vo.transaction.TransactionRequest
import org.bloqly.machine.vo.transaction.TransactionVO
import org.bloqly.machine.vo.vote.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * The main purpose of this class is to do basic preparations for requests handling
 * such as converting to model etc
 * TODO it should be reviewed as there is seem to be clash of functionality with EventProcessorService
 */
@Service
class EventReceiverService(
    private val eventProcessorService: EventProcessorService,
    private val accountService: AccountService,
    private val transactionService: TransactionService,
    private val blockService: BlockService,
    private val voteService: VoteService,
    private val objectFilterService: ObjectFilterService,
    private val spaceService: SpaceService
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactionRequest(transactionRequest: TransactionRequest): TransactionVO {

        val lastBlock = blockService.getLastBlockBySpace(transactionRequest.space)

        val lib = blockService.getLIBForBlock(lastBlock)

        val tx = transactionService.createTransaction(transactionRequest, lib.hash)

        return tx.toVO()
    }

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { tx ->
            if (!objectFilterService.mightContain(tx.hash)) {
                eventProcessorService.onTransaction(tx.toModel())
                objectFilterService.add(tx.hash)
            }
        }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs
            .filter { spaceService.existsById(it.spaceId) }
            .forEach { voteVO ->
                try {
                    if (!objectFilterService.mightContain(voteVO.getUID())) {

                        val vote = voteVO.toModel()

                        if (voteService.isAcceptable(vote)) {
                            eventProcessorService.onVote(vote)
                        }

                        objectFilterService.add(voteVO.getUID())
                    }
                } catch (e: Exception) {
                    log.error(e.message, e)
                }
            }
    }

    fun onBlocks(proposals: List<BlockData>) {

        val round = TimeUtils.getCurrentRound()

        val spaceIds = spaceService.getSpaceIds()

        proposals
            .filter { spaceService.existsById(it.block.spaceId) }
            .sortedBy { it.block.height }
            .filter { blockData ->

                val block = blockData.block

                val isNotProcessed = !objectFilterService.mightContain(block.hash)

                val isValidSpaceIds = block.spaceId in spaceIds

                val isValidRound = block.round <= round

                val isProducerValid = accountService.isProducerValidForRound(
                    block.spaceId,
                    block.producerId,
                    block.round
                )

                // TODO add TPOS check for better chain

                isProducerValid && isNotProcessed && isValidSpaceIds && isValidRound
            }
            .forEach { blockData ->

                log.info("Start processing block ${blockData.block.hash}")

                if (blockService.isAcceptable(blockData.toModel())) {
                    receiveTransactions(blockData.transactions)
                    receiveVotes(blockData.votes)

                    eventProcessorService.onProposal(blockData)
                }

                objectFilterService.add(blockData.block.hash)

            }
    }
}