package org.bloqly.machine.component

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Processes the most important events
 *
 * Q    - number of votes necessary to quorum
 * LIB  - last irreversible block
 * H    - current height
 * R    - voting round
 */
@Component
class EventProcessorService(
    private val accountService: AccountService,
    private val voteService: VoteService,
    private val transactionService: TransactionService,
    private val spaceService: SpaceService,
    private val blockProcessor: BlockProcessor,
    private val transactionProcessor: TransactionProcessor,
    private val passphraseService: PassphraseService,
    private val blockService: BlockService,
    private val objectFilterService: ObjectFilterService
) {

    private val log: Logger = LoggerFactory.getLogger(EventProcessorService::class.simpleName)

    private val blockExecutor = Executors.newSingleThreadExecutor()

    private val timeout = 1000L

    /**
     * Collecting transactions
     */
    fun onTransaction(tx: Transaction) {

        if (!transactionProcessor.isTransactionAcceptable(tx) ||
            transactionService.existsByHash(tx.hash)
        ) {
            return
        }

        try {
            transactionService.verifyAndSaveIfNotExists(tx)
        } catch (e: Exception) {
            transactionService.findByHash(tx.hash)?.let {
                log.warn("Transaction already exists ${tx.hash}")
            }
        }
    }

    /**
     * Create votes
     */
    fun onGetVotes(): List<Vote> {

        return spaceService.findAll()
            .filter { blockService.existsBySpace(it) }
            .flatMap { space ->
                accountService.getValidatorsForSpace(space)
                    .filter { passphraseService.hasPassphrase(it.accountId) }
                    .mapNotNull { validator ->
                        voteService.findVote(
                            space,
                            validator,
                            passphraseService.getPassphrase(validator.accountId)
                        )
                    }
            }
    }

    /**
     * Receive new vote
     */
    fun onVote(vote: Vote) {
        if (spaceService.existsById(vote.spaceId)) {
            try {
                voteService.findVote(vote) ?: voteService.verifyAndSave(vote)
            } catch (e: Exception) {
                voteService.findVote(vote)?.let {
                    log.warn("Vote already exists ${it.toVO()}")
                }
            }
        }
    }

    /**
     * Produce next block
     */
    fun onProduceBlock(): List<BlockData> {

        val round = TimeUtils.getCurrentRound()

        return spaceService.findAll()
            .filter { blockService.existsBySpace(it) }
            .mapNotNull { space ->
                accountService.getActiveProducerBySpace(space, round)
                    ?.let { producer ->
                        blockExecutor.submit(Callable {
                            try {
                                val blockData = createNextBlock(space, producer, round)
                                objectFilterService.add(blockData.block.hash)
                                blockData
                            } catch (e: Exception) {
                                log.error(e.message, e)
                                throw e
                            }
                        }).get(timeout, TimeUnit.MILLISECONDS)
                    }
            }
    }

    private fun createNextBlock(space: Space, producer: Account, round: Long): BlockData {
        val passphrase = passphraseService.getPassphrase(producer.accountId)

        return blockProcessor.createNextBlock(space.id, producer, passphrase, round)
    }

    /**
     * Receive block
     */
    fun onProposal(blockData: BlockData) {
        try {
            blockExecutor.submit {
                try {
                    blockProcessor.processReceivedBlock(blockData)
                } catch (e: Exception) {
                    log.error(e.message, e)
                    throw e
                }
            }.get(timeout, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            log.error("Could not process block ${blockData.block.hash}", e)
        }
    }
}