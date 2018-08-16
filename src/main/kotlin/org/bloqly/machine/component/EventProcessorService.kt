package org.bloqly.machine.component

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
    private val blockService: BlockService
) {

    private val log: Logger = LoggerFactory.getLogger(EventProcessorService::class.simpleName)

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Collecting transactions
     */
    fun onTransaction(tx: Transaction) {

        if (!transactionProcessor.isTransactionAcceptable(tx)) {
            return
        }
        executor.submit {
            transactionService.verifyAndSaveIfNotExists(tx)
        }.get(1000, TimeUnit.MILLISECONDS)
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
                        executor.submit(Callable<Vote> {
                            voteService.getVote(
                                space,
                                validator,
                                passphraseService.getPassphrase(validator.accountId)
                            )
                        }).get(1000, TimeUnit.MILLISECONDS)
                    }
            }

        // TODO send all known votes for blocks with height > H
    }

    /**
     * Receive new vote
     */
    fun onVote(vote: Vote) {
        spaceService.findById(vote.spaceId)?.let {
            executor.submit {
                voteService.validateAndSaveIfNotExists(vote)
            }.get(1000, TimeUnit.MILLISECONDS)
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
                        val passphrase = passphraseService.getPassphrase(producer.accountId)

                        // TODO exception on single block producer will stop other spaces
                        // add try/catch
                        executor.submit(Callable<BlockData> {
                            blockProcessor.createNextBlock(space.id, producer, passphrase, round)
                        }).get(1000, TimeUnit.MILLISECONDS)
                    }
            }
    }

    /**
     * Receive block
     */
    fun onProposals(proposals: List<BlockData>) {

        val round = TimeUtils.getCurrentRound()

        val spaceIds = spaceService.findAll().map { it.id }

        proposals
            .filter { it.block.round == round }
            .filter { it.block.spaceId in spaceIds }
            .filter {
                val space = spaceService.findById(it.block.spaceId)!!
                val activeValidator = accountService.getProducerBySpace(space, round)

                activeValidator.accountId == it.block.producerId
            }
            .forEach { blockData ->
                try {
                    executor.submit {
                        blockProcessor.processReceivedBlock(blockData)
                    }.get(1000, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    val errorMessage =
                        "Could not process block ${blockData.block.hash} of height ${blockData.block.height}"
                    log.warn(errorMessage)
                    log.error(errorMessage, e)
                }
            }
    }
}