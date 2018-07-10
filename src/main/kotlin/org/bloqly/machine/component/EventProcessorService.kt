package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockCandidateService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import javax.transaction.Transactional

/**
 * Processes the most important events
 *
 * Q    - number of votes necessary to quorum
 * LIB  - last irreversible block
 * H    - current height
 * BCs  - list of block candidates of height H + 1. BC contains at least Q votes, directly referring to LIB
 * R    - voting round
 *
 */
@Component
@Transactional
class EventProcessorService(
    private val contractService: ContractService,
    private val blockRepository: BlockRepository,
    private val blockService: BlockService,
    private val accountService: AccountService,
    private val spaceRepository: SpaceRepository,
    private val voteService: VoteService,
    private val voteRepository: VoteRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionService: TransactionService,
    private val blockCandidateService: BlockCandidateService,
    private val transactionProcessor: TransactionProcessor,
    private val propertyRepository: PropertyRepository
) {

    fun createBlockchain(spaceId: String, baseDir: String) {

        blockService.ensureSpaceEmpty(spaceId)

        val contractBody = File(baseDir).list()
            .filter {
                it.endsWith(".js")
            }
            .map { fileName ->
                val source = File("$baseDir/$fileName").readText()
                val extension = fileName.substringAfterLast(".")
                val header = FileUtils.getResourceAsString("/headers/header.$extension")
                header + source
            }.reduce { str, acc -> str + "\n" + acc }

        val initProperties = contractService.invokeFunction("init", contractBody)

        val rootId = initProperties.find { it.key == "root" }!!.value.toString()

        spaceRepository.save(Space(id = spaceId, creatorId = rootId))

        val timestamp = Instant.now().toEpochMilli()

        val height = 0L
        val validatorTxHash = ByteArray(0)
        val contractBodyHash = CryptoUtils.hash(contractBody).encode16()

        val firstBlock = blockService.newBlock(
            spaceId = spaceId,
            height = height,
            weight = 0,
            diff = 0,
            timestamp = timestamp,
            parentId = contractBodyHash,
            producerId = rootId,
            txHash = null,
            validatorTxHash = validatorTxHash
        )

        val transaction = transactionService.newTransaction(
            space = spaceId,
            originId = rootId,
            destinationId = DEFAULT_SELF,
            self = DEFAULT_SELF,
            key = null,
            value = contractBody.toByteArray(),
            transactionType = TransactionType.CREATE,
            referencedBlockId = firstBlock.id,
            containingBlockId = firstBlock.id,
            timestamp = timestamp
        )

        transactionProcessor.processTransaction(transaction)
        transactionRepository.save(transaction)

        firstBlock.txHash = CryptoUtils.digestTransactions(listOf(transaction))
        blockRepository.save(firstBlock)
    }

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

        return spaceRepository.findAll()
            .mapNotNull { space ->
                val round = TimeUtils.getCurrentRound()
                val lastBlock = blockRepository.getLastBlock(space.id)
                val newHeight = lastBlock.height + 1
                val producer = accountService.getActiveProducerBySpace(space, round)

                blockCandidateService.getBlockCandidate(space, newHeight, producer)
                    ?: createBlockCandidate(lastBlock, producer)
            }
    }

    private fun createBlockCandidate(lastBlock: Block, producer: Account): BlockData? {

        val newHeight = lastBlock.height + 1
        val spaceId = lastBlock.spaceId
        val votes = getVotesForBlock(lastBlock.id)
        val prevVotes = getVotesForBlock(lastBlock.parentId)

        val diff = votes.minus(prevVotes).size

        return producer
            .takeIf { it.hasKey() && isQuorum(spaceId, votes) }
            ?.let {
                val transactions = getPendingTransactions(spaceId)

                val weight = lastBlock.weight + votes.size

                val newBlock = blockService.newBlock(
                    spaceId = spaceId,
                    height = newHeight,
                    weight = weight,
                    diff = diff,
                    timestamp = Instant.now().toEpochMilli(),
                    parentId = lastBlock.id,
                    producerId = producer.id,
                    txHash = CryptoUtils.digestTransactions(transactions),
                    validatorTxHash = CryptoUtils.digestVotes(votes)
                )

                val blockData = BlockData(newBlock, transactions, votes)

                blockCandidateService.save(blockData)

                blockData
            }
    }

    private fun isQuorum(spaceId: String, votes: List<Vote>): Boolean {
        val quorum = propertyRepository.getQuorumBySpaceId(spaceId)

        return votes.count { it.id.voteType == VoteType.VOTE } >= quorum
    }

    private fun getPendingTransactions(spaceId: String): List<Transaction> {
        return transactionService.getPendingTransactionsBySpace(spaceId)
    }

    private fun getVotesForBlock(blockId: String): List<Vote> {
        return voteRepository.findByBlockId(blockId)
    }

    fun onProposals(proposals: List<BlockData>) {

        val round = TimeUtils.getCurrentRound()

        proposals
            .filter { it.block.round == TimeUtils.getCurrentRound() }
            .filter {
                val space = spaceRepository.findById(it.block.spaceId).orElseThrow()
                val activeValidator = accountService.getActiveProducerBySpace(space, round)

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