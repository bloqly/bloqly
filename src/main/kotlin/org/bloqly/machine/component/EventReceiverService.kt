package org.bloqly.machine.component

import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.TransactionRequest
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventReceiverService(
    private val eventProcessorService: EventProcessorService,
    private val accountService: AccountService,
    private val transactionService: TransactionService,
    private val blockService: BlockService
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveTransactionRequest(transactionRequest: TransactionRequest): TransactionVO {

        val lastBlock = blockService.getLastBlockForSpace(transactionRequest.space)

        val tx = transactionService.createTransaction(transactionRequest, lastBlock.libHash)

        return tx.toVO()
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { vote ->
            try {
                val validator = accountService.getByPublicKey(vote.publicKey)
                eventProcessorService.onVote(vote.toModel(validator))
            } catch (e: Exception) {
                val errorMessage = "Could not process vote $vote"
                log.warn(errorMessage)
                log.error(errorMessage, e)
            }
        }
    }

    fun receiveProposals(proposals: List<BlockData>) {
        eventProcessorService.onProposals(proposals)
    }
}