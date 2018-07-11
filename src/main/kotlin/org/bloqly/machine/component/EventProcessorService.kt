package org.bloqly.machine.component

import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockCandidateService
import org.bloqly.machine.service.TransactionService
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
    private val transactionService: TransactionService,
    private val blockCandidateService: BlockCandidateService,
    private val propertyRepository: PropertyRepository,
    private val blockProcessor: BlockProcessor
) {

    /**
     * Collecting transactions
     *
     */
    fun onTransaction(transaction: Transaction) {

        val now = Instant.now().toEpochMilli()

        if (
            transaction.timestamp > now ||
            !CryptoUtils.verifyTransaction(transaction) ||
            transactionRepository.existsById(transaction.id) ||
            !blockRepository.existsById(transaction.referencedBlockId) ||
            !transactionService.isActual(transaction)
        ) {
            return
        }

        transactionRepository.save(transaction)
    }

    /**
     * Create votes
     *
     * - It is not allowed to vote > 1 time for the same round
     * - It is not allowed to vote > 1 time for the same height
     *
     * IF already voted for H + 1 THEN
     *  vote = existing vote for H + 1
     * ELSE IF exists BCs
     *  BC = select one from BCs with min R
     *  vote = vote for BC
     * ELSE
     *  vote for the LIB
     *
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
     *
     * validate
     * save
     *
     * If it is a valid vote for an unknown block - OK, return
     * If it is a valid vote for LIB, H, - OK, return
     *
     *
     */
    fun onVote(vote: Vote) {
        voteService.validateAndSave(vote)
    }

    /**
     * Get next block proposal
     */
    fun onGetProposals(): List<BlockData> {

        val round = TimeUtils.getCurrentRound()

        return spaceRepository.findAll()
            .mapNotNull { space ->
                accountService.getActiveProducerBySpace(space, round)?.let { producer ->
                    blockProcessor.createNextBlock(space.id, producer, round)
                }
            }
    }

    private fun isQuorum(spaceId: String, votes: List<Vote>): Boolean {
        val quorum = propertyRepository.getQuorumBySpaceId(spaceId)

        return votes.count { it.id.voteType == VoteType.VOTE } >= quorum
    }

    fun onProposals(proposals: List<BlockData>) {

        val round = TimeUtils.getCurrentRound()

        proposals
            .filter { it.block.round == TimeUtils.getCurrentRound() }
            .filter {
                val space = spaceRepository.findById(it.block.spaceId).orElseThrow()
                val activeValidator = accountService.getProducerBySpace(space, round)

                activeValidator.id == it.block.proposerId
            }
            .filter { proposal ->
                val quorum = propertyRepository.getQuorumBySpaceId(proposal.block.spaceId)
                proposal.votes.count { it.voteType == VoteType.VOTE.name } >= quorum
            }
            .forEach { blockCandidateService.validateAndSave(it) }
    }

    /**
     * If it is a valid vote for BC, H + 1 AND BC reached Q then LIB = BC
     *
     */
    fun onTick() {
        spaceRepository.findAll()
            .forEach { space ->
                val lastBlock = blockRepository.getLastBlock(space.id)
                val newHeight = lastBlock.height + 1

                blockCandidateService.getBestBlockCandidate(space, newHeight)
                    ?.let { blockRepository.save(it.block.toModel()) }
            }
    }
}