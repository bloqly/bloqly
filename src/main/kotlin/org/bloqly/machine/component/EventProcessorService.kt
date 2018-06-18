package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
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
import org.bloqly.machine.vo.BlockData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import javax.transaction.Transactional

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

    fun createBlockchain(space: String, baseDir: String) {

        blockService.ensureSpaceEmpty(space)

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

        spaceRepository.save(Space(id = space, creatorId = rootId))

        val timestamp = Instant.now().toEpochMilli()

        val height = 0L
        val validatorTxHash = ByteArray(0)
        val contractBodyHash = EncodingUtils.encodeToString16(
            CryptoUtils.digest(contractBody)
        )

        val firstBlock = blockService.newBlock(
            space = space,
            height = height,
            timestamp = timestamp,
            parentHash = contractBodyHash,
            proposerId = rootId,
            txHash = null,
            validatorTxHash = validatorTxHash
        )

        val transaction = transactionService.newTransaction(
            space = space,
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
            !CryptoUtils.isTransactionValid(transaction) ||
            transactionRepository.existsById(transaction.id) ||
            !blockRepository.existsById(transaction.referencedBlockId) ||
            !transactionService.isActual(transaction)
        ) {
            return
        }

        transactionRepository.save(transaction)
    }

    /**
     * Step 1.2, vote for previous block
     *
     */
    fun onGetVotes(): List<Vote> {

        val spaces = spaceRepository.findAll().map { it.id }

        return spaces.flatMap { space ->
            val validators = accountService.getValidatorsForSpace(space)

            validators
                .filter { it.privateKey != null }
                .map { validator -> voteService.createVote(space, validator) }
        }
    }

    /**
     * Step 1.3, collecting votes
     *
     */
    fun onVote(vote: Vote) {

        require(CryptoUtils.verifyVote(vote)) {
            "Could not verify vote $vote"
        }

        voteRepository.save(vote)
    }

    /**
     * Step 2, get next block proposal
     */
    // TODO return existing proposals if available
    fun onGetProposals(): List<BlockData> {

        val spaces = spaceRepository.findAll().map { it.id }

        val proposals = spaces
            .mapNotNull { space ->

                val lastBlock = blockRepository.getLastBlock(space)
                val transactions = getPendingTransactions(space)
                val votes = getNewVotes(space)
                val validator = accountService.getActiveValidator(space, lastBlock.height + 1)

                validator.privateKey?.let {

                    val proposal = blockService.newBlock(
                        space = space,
                        height = lastBlock.height + 1,
                        timestamp = Instant.now().toEpochMilli(),
                        parentHash = lastBlock.id,
                        proposerId = validator.id,
                        txHash = CryptoUtils.digestTransactions(transactions),
                        validatorTxHash = CryptoUtils.digestVotes(votes)
                    )

                    BlockData(proposal, transactions, votes)
                }
            }
            .filter { blockData ->
                val quorum = propertyRepository.getQuorum(blockData.block.space)

                blockData.votes.size >= quorum
            }
            .toList()

        if (proposals.isNotEmpty()) {
            blockCandidateService.saveAll(proposals)
        }

        return proposals
    }

    private fun getPendingTransactions(space: String): List<Transaction> {

        //val lastBlock = blockRepository.getLastBlock(space)
        //val height = lastBlock.height
        // TODO

        return transactionRepository.findBySpaceAndContainingBlockIdIsNull(space)
    }

    private fun getNewVotes(space: String): List<Vote> {

        val lastBlock = blockRepository.getLastBlock(space)

        return voteRepository.findByBlockId(lastBlock.id)
    }

    fun onProposals(proposals: List<BlockData>) {

        blockCandidateService.saveAll(proposals)
    }

    fun onSelectBestProposal(): List<BlockData> {

        val spaces = spaceRepository.findAll().map { it.id }

        return spaces.mapNotNull { space ->
            val lastBlock = blockRepository.getLastBlock(space)

            val validator = accountService.getActiveValidator(space, lastBlock.height + 1)

            val bestBlockCandidate = blockCandidateService
                .getBlockCandidate(space, lastBlock.height + 1, validator.id)

            bestBlockCandidate?.let {
                val block = bestBlockCandidate.block

                log.info("Selected next block ${block.id} on height ${block.height}.")

                blockRepository.save(block.toModel())
            }

            bestBlockCandidate
        }
    }
}