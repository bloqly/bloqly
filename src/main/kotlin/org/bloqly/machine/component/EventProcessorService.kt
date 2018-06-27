package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.model.Vote
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
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.TimeUtils
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

        val rootId = initProperties.find { it.middle == "root" }!!.right.toString()

        spaceRepository.save(Space(id = spaceId, creatorId = rootId))

        val timestamp = Instant.now().toEpochMilli()

        val height = 0L
        val validatorTxHash = ByteArray(0)
        val contractBodyHash = EncodingUtils.encodeToString16(
            CryptoUtils.digest(contractBody)
        )

        val firstBlock = blockService.newBlock(
            spaceId = spaceId,
            height = height,
            timestamp = timestamp,
            parentHash = contractBodyHash,
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

    fun processTransaction(transaction: Transaction) {

        // contract id
        val self = transaction.self ?: DEFAULT_SELF

        // contract function name
        val key = transaction.key ?: DEFAULT_FUNCTION_NAME

        // contract arguments
        val arg = transaction.value
        val caller = transaction.origin
        val callee = transaction.destination

        contractService.invokeContract(key, self, caller, callee, arg)
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
                    .map { validator -> voteService.getVote(space, validator) }
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

    private fun createBlockCandidate(
        lastBlock: Block,
        producer: Account
    ): BlockData? {
        val newHeight = lastBlock.height + 1
        val spaceId = lastBlock.spaceId
        val votes = getVotesForBlock(lastBlock)

        return producer
            .takeIf { it.hasKey() && isQuorum(spaceId, votes) }
            ?.let {
                val transactions = getPendingTransactions(spaceId)

                val newBlock = blockService.newBlock(
                    spaceId = spaceId,
                    height = newHeight,
                    timestamp = Instant.now().toEpochMilli(),
                    parentHash = lastBlock.id,
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

        return votes.size >= quorum
    }

    private fun getPendingTransactions(spaceId: String): List<Transaction> {
        return transactionService.getPendingTransactionsBySpace(spaceId)
    }

    private fun getVotesForBlock(block: Block): List<Vote> {
        return voteRepository.findByBlockId(block.id)
    }

    fun onProposals(proposals: List<BlockData>) {
        proposals.forEach { blockData ->

            val round = TimeUtils.getCurrentRound()
            val spaceId = blockData.block.spaceId
            val space = spaceRepository.findById(spaceId).orElseThrow()
            val activeValidator = accountService.getActiveProducerBySpace(space, round)

            if (activeValidator.id == blockData.block.proposerId) {
                blockCandidateService.validateAndSave(blockData)
            } else {
                // TODO log illegal request
            }
        }
    }

    /**
     * If it is a valid vote for BC, H + 1 AND BC reached Q then LIB = BC
     *
     */
    fun onSelectBestProposal() {
        spaceRepository.findAll()
            .forEach { space ->
                val lastBlock = blockRepository.getLastBlock(space.id)
                val newHeight = lastBlock.height + 1
                val newHeightVotes = voteRepository.findByHeight(newHeight)

                if (isDeadlock(space, newHeightVotes)) {
                    blockRepository.save(newLockBlock(space, lastBlock))
                } else {
                    blockCandidateService.getBestBlockCandidate(space, newHeight)
                        ?.let { blockRepository.save(it.block.toModel()) }
                }
            }
    }

    private fun newLockBlock(
        space: Space,
        lastBlock: Block
    ): Block {

        val newHeight = lastBlock.height + 1

        val lockBlockId = EncodingUtils.encodeToString16(
            CryptoUtils.digest("${space.id}:$newHeight")
        )

        return Block(
            id = lockBlockId,
            spaceId = space.id,
            height = newHeight,
            round = -1,
            timestamp = Instant.now().toEpochMilli(),
            parentHash = lastBlock.id,
            proposerId = lockBlockId
        )
    }

    private fun isDeadlock(space: Space, newHeightVotes: List<Vote>): Boolean {
        val proposalIds = newHeightVotes.map { it.blockId }.toSet()
        val quorum = propertyRepository.getQuorumBySpaceId(space.id)
        val validators = accountService.getValidatorsForSpace(space)

        return newHeightVotes.size >= quorum && proposalIds.size > validators.size - quorum + 1
    }
}