package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(EventProcessorService::class.simpleName)

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
     * Step 1.1, collecting transactions
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
    }

    /**
     * Step 1.3, collecting votes
     *
     */
    fun onVote(vote: Vote) {
        voteService.processVote(vote)
    }

    /**
     * Step 2, get next block proposal
     */
    // TODO return existing proposals if available
    fun onGetProposals(): List<BlockData> {

        return spaceRepository
            .findAll()
            .mapNotNull { space ->

                val round = TimeUtils.getCurrentRound()

                val lastBlock = blockRepository.getLastBlock(space.id)
                val newHeight = lastBlock.height + 1
                val producer = accountService.getActiveProducerBySpace(space, round)

                val savedProposal = blockCandidateService.getBlockCandidate(
                    space = space,
                    height = newHeight,
                    round = round,
                    producerId = producer.id
                )

                if (savedProposal != null) {
                    savedProposal
                } else {
                    val transactions = getPendingTransactions(space)
                    val votes = getVotesForBlock(lastBlock)

                    producer.privateKey?.let {

                        val newBlock = blockService.newBlock(
                            spaceId = space.id,
                            height = newHeight,
                            timestamp = Instant.now().toEpochMilli(),
                            parentHash = lastBlock.id,
                            producerId = producer.id,
                            txHash = CryptoUtils.digestTransactions(transactions),
                            validatorTxHash = CryptoUtils.digestVotes(votes)
                        )

                        BlockData(newBlock, transactions, votes)
                    }
                }
            }
            .filter { hasQuorum(it) }
            .onEach { blockCandidateService.save(it) }
    }

    private fun hasQuorum(blockData: BlockData): Boolean {

        val quorum = propertyRepository.getQuorumBySpaceId(blockData.block.spaceId)

        return blockData.votes.size >= quorum
    }

    private fun getPendingTransactions(space: Space): List<Transaction> {
        return transactionService.getPendingTransactionsBySpace(space)
    }

    private fun getVotesForBlock(block: Block): List<Vote> {
        return voteRepository.findByBlockId(block.id)
    }

    fun onProposals(proposals: List<BlockData>) {
        proposals.forEach { blockCandidateService.save(it) }
    }

    fun onSelectBestProposal(): List<BlockData> {
        return spaceRepository
            .findAll()
            .mapNotNull { space ->
                val lastBlock = blockRepository.getLastBlock(space.id)
                val newHeight = lastBlock.height + 1

                val round = TimeUtils.getCurrentRound()

                // what is the active validator for the current round in this spaceId?
                val producer = accountService.getActiveProducerBySpace(space, round)

                // did this validator produce a block candidate we are aware of?
                val blockCandidate = blockCandidateService.getBlockCandidate(space, newHeight, round, producer.id)

                if (blockCandidate != null) {
                    // block candidate is found, select it

                    val block = blockCandidate.block

                    log.info("Selected next block ${block.id} on height ${block.height}.")

                    blockRepository.save(block.toModel())
                }

                blockCandidate
            }
    }
}