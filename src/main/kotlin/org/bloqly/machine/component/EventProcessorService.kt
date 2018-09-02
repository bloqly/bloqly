package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.BLOCK_TIMEOUT
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.block.BlockData
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

    private val executor = Executors.newSingleThreadExecutor()

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
            submitTask {
                transactionService.verifyAndSaveIfNotExists(tx)
            }
        } catch (e: Exception) {
            transactionService.findByHash(tx.hash)?.let {
                log.warn("Transaction already exists ${tx.hash}")
            }
        }
    }

    /**
     * Create votes
     *
     * Validator can create single vote per height
     */
    fun onGetVotes(): List<Vote> {

        return spaceService.findAll()
            .filter { blockService.existsBySpace(it) }
            .mapNotNull { space ->
                accountService.findValidatorsForSpace(space)?.let { validators ->
                    validators
                        .filter { passphraseService.hasPassphrase(it.accountId) }
                        .filter { it.privateKeyEncoded != null }
                        .mapNotNull { producer ->
                            submitTask {
                                voteService.findOrCreateVote(
                                    space,
                                    producer,
                                    passphraseService.getPassphrase(producer.accountId)
                                )
                            }
                        }
                }
            }
            .flatMap { it }
    }

    /**
     * Receive new vote
     */
    fun onVote(vote: Vote) {
        try {
            submitTask { voteService.verifyAndSave(vote) }
        } catch (e: Exception) {
            log.error("Could not process vote ${vote.toVO()}", e)
        }
    }

    /**
     * Produce next block
     *
     * Create next block or return the one created by current active producer in current round
     */
    fun onProduceBlock(): List<BlockData> {

        val round = TimeUtils.getCurrentRound()

        return spaceService.findAll()
            .filter { blockService.existsBySpace(it) }
            .mapNotNull { space ->
                try {
                    accountService.getActiveProducerBySpace(space, round)
                        ?.let { producer ->
                            submitTask { createNextBlock(space.id, producer, round) }
                        }
                } catch (e: Exception) {
                    log.error("Could not produce block for round $round", e)
                    null
                }
            }
            .onEach { blockData -> objectFilterService.add(blockData.block.hash) }
    }

    fun createNextBlock(spaceId: String, producer: Account, round: Long): BlockData {
        val lastBlock = blockService.getLastBlockBySpace(spaceId)

        return createNextBlock(lastBlock, producer, round)
    }

    fun createNextBlock(lastBlock: Block, producer: Account, round: Long): BlockData {

        val passphrase = passphraseService.getPassphrase(producer.accountId)

        val pendingTransactions = blockProcessor.getPendingTransactions(lastBlock)

        return blockProcessor.createNextBlock(
            lastBlock.hash, pendingTransactions, producer, passphrase, round
        )
    }

    /**
     * Receive block
     */
    fun onProposal(blockData: BlockData) {
        try {
            submitTask {
                blockProcessor.processReceivedBlock(blockData)
            }
        } catch (e: Exception) {
            log.error("Could not process block ${blockData.block.hash}", e)
        }
    }

    private fun <T> submitTask(task: () -> T): T {
        return executor.submit(Callable<T> {
            try {
                task()
            } catch (e: Exception) {
                log.error(e.message, e)
                throw e
            }
        }).get(BLOCK_TIMEOUT, TimeUnit.MILLISECONDS)
    }
}