package org.bloqly.machine.component


import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.exception.SpaceAlreadyExistsException
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.BlockData
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.FileUtils
import org.springframework.stereotype.Component
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.transaction.Transactional

@Component
@Transactional
class EventProcessorService(

    private val contractService: ContractService,
    private val blockRepository: BlockRepository,
    private val blockService: BlockService,
    private val cryptoService: CryptoService,
    private val accountService: AccountService,
    private val spaceRepository: SpaceRepository,
    private val voteService: VoteService,
    private val accountRepository: AccountRepository,
    private val voteRepository: VoteRepository,
    private val transactionRepository: TransactionRepository,
    private val propertyService: PropertyService

) {

    private val newProposals: MutableSet<BlockData> = mutableSetOf()

    fun createBlockchain(space: String, baseDir: String) {

        ensureSpaceEmpty(space)

        val accounts = accountService.readAccounts(baseDir)

        accounts.forEach {
            accountService.importAccount(
                    publicKey = it.publicKey,
                    privateKey = it.privateKey
            )
        }

        var scriptSource = File(baseDir).list()
                .filter {
                    it.endsWith(".js")
                }
                .map { fileName ->

                    val source = File("$baseDir/$fileName").readText()

                    val extension = fileName.substringAfterLast(".")

                    val header = FileUtils.getResourceAsString("/headers/header.$extension")

                    header + source

                }.reduce { str, acc -> str + acc }

        var i = 1

        do {
            val placeholder = "{{address$i}}"

            if (!scriptSource.contains(placeholder)) {
                break
            }

            scriptSource = scriptSource.replace(placeholder, accounts[i].id)

            i++

        } while (true)

        onCreate(accounts.first(), space, scriptSource)
    }

    private fun onCreate(root: Account,
                         space: String,
                         defaultContractBody: String) {

        contractService.createContract(
                space,
                DEFAULT_SELF,
                root.id, // owner
                defaultContractBody
        )

        spaceRepository.save(
                Space(
                        id = space,
                        creatorId = root.id
                )
        )

        val parentHash = "" // there is no parent
        val height = 0L
        val proposerId = root.id
        val timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()
        val txHash = ByteArray(0)
        val validatorTxHash = ByteArray(0)

        val firstBlock = blockService.newBlock(
                space = space,
                height = height,
                timestamp = timestamp,
                parentHash = parentHash,
                proposerId = proposerId,
                txHash = txHash,
                validatorTxHash = validatorTxHash
        )

        blockRepository.save(firstBlock)
    }

    private fun ensureSpaceEmpty(space: String) {

        val blocksExists = blockRepository.existsBySpace(space)

        if (blocksExists) {
            throw SpaceAlreadyExistsException("Blockchain already initialized for space $space")
        }
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

        // TODO: check also timestamp

        if (
                !cryptoService.verifyTransaction(transaction) ||
                transactionRepository.existsById(transaction.id) ||
                !blockRepository.existsById(transaction.referencedBlockId)) {
            return
        }

        // TODO verify transaction can be executed
        transactionRepository.save(transaction)
    }

    /**
     * Step 1.2, vote for previous block
     *
     */
    fun onGetVote(): List<Vote> {

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

        val validatorOpt = accountRepository.findById(vote.id.validatorId)

        validatorOpt.ifPresent { validator ->

            if (!cryptoService.verifyVote(validator, vote)) {
                throw IllegalArgumentException("Could not verify vote $vote")
            }

            voteRepository.save(vote)
        }
    }

    /**
     * Step 2, get next block proposal
     */
    fun onGetProposals(): Set<BlockData> {

        val spaces = spaceRepository.findAll().map { it.id }

        val proposals = spaces.flatMap { space ->

            val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space)
            val transactions = getNewTransactions(space)
            val votes = getNewVotes(space)
            val validators = accountService.getValidatorsForSpace(space)
                    .filter { it.privateKey != null }

            val newBlocks = validators.map { validator ->

                blockService.newBlock(
                        space = space,
                        height = lastBlock.height + 1,
                        timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond(),
                        parentHash = lastBlock.id,
                        proposerId = validator.id,
                        txHash = cryptoService.digestTransactions(transactions),
                        validatorTxHash = cryptoService.digestVotes(votes)
                )
            }

            newBlocks.map { newBlock -> BlockData(newBlock, transactions, votes) }

        }.toSet()

        newProposals.addAll(proposals)

        return proposals
    }

    private fun getNewTransactions(space: String): List<Transaction> {

        return transactionRepository.findBySpaceAndContainingBlockIdIsNull(space)
    }

    private fun getNewVotes(space: String): List<Vote> {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space)

        return voteRepository.findByBlockId(lastBlock.id)
    }

    fun onProposals(proposals: List<BlockData>) {

        newProposals.addAll(proposals)
    }

    fun onSelectBestProposal(): List<BlockData> {

        val spaces = spaceRepository.findAll().map { it.id }

        return spaces.mapNotNull { space ->

            val quorum = propertyService.getQuorum(space)
            val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space)

            // TODO add sort
            // TODO add validators schedule check
            // TODO how to count power?
            val bestProposal = newProposals
                    .sortedWith(
                            compareByDescending<BlockData> { it.votes.size }
                                    .thenByDescending { it.transactions.size }
                                    .thenByDescending { it.block.id }
                    )
                    .firstOrNull {
                        it.block.space == space && it.votes.size >= quorum && it.block.height == lastBlock.height + 1
                    }


            bestProposal?.let {
                transactionRepository.saveAll(it.transactions)
                blockRepository.save(it.block)
                voteRepository.saveAll(it.votes)
            }

            bestProposal
        }
    }
}