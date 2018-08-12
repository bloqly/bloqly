package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

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
    private val blockService: BlockService,
    private val voteService: VoteService,
    private val transactionService: TransactionService,
    private val spaceService: SpaceService,
    private val blockProcessor: BlockProcessor,
    private val blockchainService: BlockchainService,
    private val passphraseService: PassphraseService
) {

    private val log: Logger = LoggerFactory.getLogger(EventProcessorService::class.simpleName)

    /**
     * Collecting transactions
     */
    fun onTransaction(tx: Transaction) {

        val now = Instant.now().toEpochMilli()

        // TODO move to a separate method
        if (
            tx.timestamp > now ||
            !CryptoUtils.verifyTransaction(tx) ||
            transactionService.existsByHash(tx.hash) ||
            !blockService.existsByHash(tx.referencedBlockHash) ||
            !blockchainService.isActualTransaction(tx, MAX_REFERENCED_BLOCK_DEPTH) ||
            transactionService.existsByNonce(tx.nonce)
        ) {
            return
        }

        transactionService.save(tx)
    }

    /**
     * Create votes
     */
    fun onGetVotes(): List<Vote> {
        return spaceService.findAll()
            .flatMap { space ->
                accountService.getValidatorsForSpace(space)
                    .filter { passphraseService.hasPassphrase(it.accountId) }
                    .mapNotNull { validator ->
                        voteService.getVote(
                            space,
                            validator,
                            passphraseService.getPassphrase(validator.accountId)
                        )
                    }
            }

        // TODO send all known votes for blocks with height > H
    }

    /**
     * Receive new vote
     */
    fun onVote(vote: Vote) {
        spaceService.findById(vote.spaceId)?.let {
            voteService.validateAndSave(vote)
        }
    }

    /**
     * Produce next block
     */
    fun onProduceBlock(): List<BlockData> {

        val round = TimeUtils.getCurrentRound()

        return spaceService.findAll()
            .mapNotNull { space ->
                accountService.getActiveProducerBySpace(space, round)
                    ?.let { producer ->
                        val passphrase = passphraseService.getPassphrase(producer.accountId)
                        blockProcessor.createNextBlock(space.id, producer, passphrase, round)
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
                    blockProcessor.processReceivedBlock(blockData)
                } catch (e: Exception) {
                    val errorMessage =
                        "Could not process block ${blockData.block.hash} of height ${blockData.block.height}"
                    log.warn(errorMessage)
                    log.error(errorMessage, e)
                }
            }
    }
}