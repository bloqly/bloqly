package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Component
import java.time.Instant
import javax.transaction.Transactional

/**
 * Processes the most important events
 *
 * Q    - number of votes necessary to quorum
 * LIB  - last irreversible block
 * H    - current height
 * R    - voting round
 */
@Component
@Transactional
class EventProcessorService(
    private val blockRepository: BlockRepository,
    private val accountService: AccountService,
    private val spaceRepository: SpaceRepository,
    private val voteService: VoteService,
    private val transactionRepository: TransactionRepository,
    private val blockProcessor: BlockProcessor,
    private val blockchainService: BlockchainService
) {

    /**
     * Collecting transactions
     *
     */
    fun onTransaction(tx: Transaction) {

        val now = Instant.now().toEpochMilli()

        if (
            tx.timestamp > now ||
            !CryptoUtils.verifyTransaction(tx) ||
            transactionRepository.existsByHash(tx.hash!!) ||
            !blockRepository.existsByHash(tx.referencedBlockHash) ||
            !blockchainService.isActualTransaction(tx, MAX_REFERENCED_BLOCK_DEPTH)
        ) {
            return
        }

        transactionRepository.save(tx)
    }

    /**
     * Create votes
     */
    fun onGetVotes(): List<Vote> {
        return spaceRepository.findAll()
            .flatMap { space ->
                accountService.getValidatorsForSpace(space)
                    .filter { it.hasKey() }
                    .mapNotNull { validator -> voteService.getVote(space, validator) }
            }

        // TODO send all known votes for blocks with height > H
    }

    /**
     * Receive new vote
     */
    fun onVote(vote: Vote) {
        voteService.validateAndSave(vote)
    }

    /**
     * Produce next block
     */
    fun onProduceBlock(): List<BlockData> {

        val round = TimeUtils.getCurrentRound()

        return spaceRepository.findAll()
            .mapNotNull { space ->
                accountService.getActiveProducerBySpace(space, round)
                    ?.let { producer ->
                        blockProcessor.createNextBlock(space.id, producer, round)
                    }
            }
    }

    /**
     * Receive block
     */
    fun onProposals(proposals: List<BlockData>) {

        val round = TimeUtils.getCurrentRound()

        proposals
            .filter { it.block.round == round }
            .filter {
                val space = spaceRepository.findById(it.block.spaceId).orElseThrow()
                val activeValidator = accountService.getProducerBySpace(space, round)

                activeValidator.id == it.block.producerId
            }
            .forEach { blockProcessor.processReceivedBlock(it) }
    }
}