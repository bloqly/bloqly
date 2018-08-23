package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.MAX_TRANSACTION_AGE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.FinalizedTransaction
import org.bloqly.machine.model.InvocationResult
import org.bloqly.machine.model.Properties
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionOutput
import org.bloqly.machine.model.TransactionOutputId
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.FinalizedTransactionRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.BlockData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private data class TransactionResult(
    val transaction: Transaction,
    val invocationResult: InvocationResult
)

@Service
// TODO add more test for rejected transaction
class BlockProcessor(
    private val transactionService: TransactionService,
    private val voteService: VoteService,
    private val voteRepository: VoteRepository,
    private val blockService: BlockService,
    private val blockRepository: BlockRepository,
    private val transactionProcessor: TransactionProcessor,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val transactionOutputRepository: TransactionOutputRepository,
    private val accountRepository: AccountRepository,
    private val spaceService: SpaceService,
    private val transactionRepository: TransactionRepository,
    private val finalizedTransactionRepository: FinalizedTransactionRepository,
    private val passphraseService: PassphraseService
) {

    private val log: Logger = LoggerFactory.getLogger(BlockProcessor::class.simpleName)

    @Transactional
    fun processReceivedBlock(blockData: BlockData) {

        log.info("Processing received block: ${blockData.block}")

        val receivedBlock = blockData.block.toModel()

        requireValid(receivedBlock)

        val lastBlock = blockService.getLastBlockBySpace(receivedBlock.spaceId)

        val currentLIB = blockService.calculateLIBForBlock(lastBlock)

        val votes = blockData.votes.map { vote ->
            try {
                voteService.findVote(vote.publicKey, vote.blockHash)!!
            } catch (e: Exception) {
                log.error("Could not find saved vote $vote")
                throw e
            }
        }

        val transactions = blockData.transactions.map { tx ->
            try {
                transactionService.findByHash(tx.hash)!!
            } catch (e: Exception) {
                log.error("Could not find saved transaction $tx")
                throw e
            }
        }

        val propertyContext = PropertyContext(propertyService, contractService)

        evaluateBlocks(currentLIB, receivedBlock, propertyContext)

        val block = receivedBlock.copy(
            transactions = transactions,
            votes = votes
        )

        evaluateBlock(block, propertyContext)

        log.debug("Saving block with hash ${block.hash}")
        val savedNewBlock = saveBlock(block)
        log.debug("Saved block with hash ${block.hash}")

        moveLIBIfNeeded(currentLIB, savedNewBlock)
    }

    private fun saveBlock(block: Block): Block {
        require(blockService.isAcceptable(block)) {
            // TODO improve block logging, add producer etc
            "Block is not acceptable: ${block.hash}"
        }
        return blockRepository.save(block)
    }

    private fun evaluateBlocks(currentLIB: Block, toBlock: Block, propertyContext: PropertyContext) {

        if (currentLIB == toBlock) {
            return
        }

        getBlocksRange(currentLIB, toBlock).forEach { block ->
            evaluateBlock(block, propertyContext)
        }
    }

    @Transactional(readOnly = true)
    fun getLastPropertyValue(
        spaceId: String,
        self: String,
        target: String,
        key: String
    ): ByteArray? {
        val lastBlock = blockService.getLastBlockBySpace(spaceId)
        val lib = blockService.getByHash(lastBlock.libHash)

        val propertyContext = PropertyContext(propertyService, contractService)

        evaluateBlocks(lib, lastBlock, propertyContext)

        return propertyContext.getPropertyValue(spaceId, self, target, key)
    }

    /**
     * Returns blocks range (afterBlock, toBlock]
     */
    @Transactional(readOnly = true)
    internal fun getBlocksRange(afterBlock: Block, toBlock: Block): List<Block> {

        var currentBlock = toBlock

        val blocks = mutableListOf<Block>()

        while (currentBlock.height > afterBlock.height) {
            blocks.add(currentBlock)

            currentBlock = blockRepository.findByHash(currentBlock.parentHash)!!
        }

        return blocks.reversed()
    }

    private fun evaluateBlock(block: Block, propertyContext: PropertyContext) {

        block.transactions.forEach { tx ->

            // already processed this transaction?
            val txOutput = transactionOutputRepository
                .findById(TransactionOutputId(block.hash, tx.hash))

            val output = if (txOutput.isPresent) {
                ObjectUtils.readProperties(txOutput.get().output)
            } else {
                val invocationResult = transactionProcessor.processTransaction(tx, propertyContext)

                require(invocationResult.isOK()) {
                    "Could not process transaction $tx"
                }

                saveTxOutputs(listOf(TransactionResult(tx, invocationResult)), block)

                invocationResult.output
            }

            propertyContext.updatePropertyValues(output)
        }
    }

    private fun requireValid(block: Block) {

        val producer = accountRepository.findByAccountId(block.producerId)!!

        require(CryptoUtils.verifyBlock(block, producer.publicKey.decode16())) {
            "Cold not verify block ${block.hash}"
        }
    }

    @Transactional
    fun createNextBlock(spaceId: String, producer: Account, round: Long): BlockData {
        val lastBlock = blockService.getLastBlockBySpace(spaceId)

        val passphrase = passphraseService.getPassphrase(producer.accountId)

        return createNextBlock(lastBlock, producer, passphrase, round)
    }

    @Transactional
    fun createNextBlock(lastBlock: Block, producer: Account, round: Long): BlockData {

        val passphrase = passphraseService.getPassphrase(producer.accountId)

        return createNextBlock(lastBlock, producer, passphrase, round)
    }

    private fun createNextBlock(lastBlock: Block, producer: Account, passphrase: String, round: Long): BlockData {

        log.info("Creating next block on top of ${lastBlock.header()}")

        val spaceId = lastBlock.spaceId

        // TODO add height to equation?
        blockRepository.findBySpaceIdAndProducerIdAndRound(spaceId, producer.accountId, round)
            ?.let { return BlockData(it) }

        val newHeight = lastBlock.height + 1

        val currentLIB = blockService.getByHash(lastBlock.libHash)

        val propertyContext = PropertyContext(propertyService, contractService)

        val t1 = System.currentTimeMillis()
        evaluateBlocks(currentLIB, lastBlock, propertyContext)
        val t2 = System.currentTimeMillis()
        log.trace("TIME SPENT EVALUATE: " + (t2 - t1))

        val txResults = getTransactionResultsForNextBlock(lastBlock, propertyContext)
        val t3 = System.currentTimeMillis()
        log.trace("TIME SPENT EVALUATE: " + (t3 - t2))

        val transactions = txResults.map { it.transaction }

        val votes = getVotesForBlock(lastBlock.hash)
        val prevVotes = getVotesForBlock(lastBlock.parentHash)
        val t4 = System.currentTimeMillis()
        log.trace("TIME SPENT LOAD VOTES AND TRANSACTIONS: " + (t4 - t3))

        val diff = votes.minus(prevVotes).size
        val weight = lastBlock.weight + votes.size

        val newBlock = blockService.newBlock(
            spaceId = spaceId,
            height = newHeight,
            weight = weight,
            diff = diff,
            timestamp = TimeUtils.getCurrentTime(),
            parentHash = lastBlock.hash,
            producerId = producer.accountId,
            passphrase = passphrase,
            txHash = CryptoUtils.hashTransactions(transactions),
            validatorTxHash = CryptoUtils.hashVotes(votes),
            round = round,
            transactions = transactions,
            votes = votes
        )
        val t5 = System.currentTimeMillis()
        log.trace("TIME SPENT CREATE NEW: " + (t5 - t4))

        saveTxOutputs(txResults, newBlock)

        val t6 = System.currentTimeMillis()
        log.trace("TIME SPENT saveTxOutputs: " + (t6 - t5))

        val blockData = BlockData(saveBlock(newBlock))

        moveLIBIfNeeded(currentLIB, newBlock)

        val t7 = System.currentTimeMillis()
        log.trace("TIME SPENT moveLIBIfNeeded: " + (t7 - t6))

        return blockData
    }

    // TODO validate block

    /**
     * Apply transaction outputs if LIB moved forward
     * Iterate and apply all transactions from the block next to the previous LIB including NEW_LIB
     * In some situations LIB.height + 1 = NEW_LIB.height
     */
    private fun moveLIBIfNeeded(currentLIB: Block, lastBlock: Block) {

        log.info("Check if LIB has changed, currentLIB: ${currentLIB.header()}, last block: ${lastBlock.header()}")

        val newLIB = blockService.getByHash(lastBlock.libHash)

        if (newLIB == currentLIB || newLIB.height <= currentLIB.height) {
            return
        }

        log.info(
            """
                Moving LIB from height ${currentLIB.height} to ${newLIB.height}.
                currentLIB.hash = ${currentLIB.hash}, newLIB.hash = ${newLIB.hash}.""".trimIndent()
        )

        getBlocksRange(currentLIB, newLIB).forEach { block ->
            block.transactions.forEach { tx ->

                try {

                    val txOutput = transactionOutputRepository
                        .findById(TransactionOutputId(block.hash, tx.hash))
                        .orElseThrow()

                    val properties = ObjectUtils.readProperties(txOutput.output)

                    propertyService.updateProperties(properties)

                    finalizedTransactionRepository.save(
                        FinalizedTransaction(
                            transaction = tx,
                            block = block
                        )
                    )
                } catch (e: Exception) {
                    val errorMessage =
                        "Could not process transaction output tx: ${tx.hash}, block: ${block.hash}"
                    log.warn(errorMessage, e)
                    throw RuntimeException(errorMessage, e)
                }
            }
        }
    }

    private fun saveTxOutputs(txResults: List<TransactionResult>, block: Block) {
        txResults.forEach { txResult ->

            val output = ObjectUtils.writeValueAsString(
                Properties(txResult.invocationResult.output)
            )

            val txOutput = TransactionOutput(
                TransactionOutputId(block.hash, txResult.transaction.hash),
                output
            )
            transactionOutputRepository.save(txOutput)
        }
    }

    private fun getTransactionResultsForNextBlock(
        lastBlock: Block,
        propertyContext: PropertyContext
    ): List<TransactionResult> {
        val transactions = getPendingTransactionsByLastBlock(lastBlock)

        val hashes = transactions.map { it.hash }

        if (hashes.size > hashes.distinct().size) {
            throw IllegalStateException("Transactions are not unique")
        }

        return transactions
            .map { tx ->

                val localPropertyContext = propertyContext.getLocalCopy()

                val result = transactionProcessor.processTransaction(tx, localPropertyContext)

                if (result.isOK()) {
                    propertyContext.merge(localPropertyContext)
                }

                TransactionResult(tx, result)
            }
            .filter { it.invocationResult.isOK() }
    }

    private fun getVotesForBlock(blockHash: String): List<Vote> {
        return voteRepository.findByBlockHash(blockHash)
    }

    @Transactional(readOnly = true)
    fun getPendingTransactions(depth: Int = Application.MAX_REFERENCED_BLOCK_DEPTH): List<Transaction> =
        spaceService.findAll().flatMap {
            val lastBlock = blockRepository.getLastBlock(it.id)
            getPendingTransactionsByLastBlock(lastBlock, depth)
        }

    @Transactional(readOnly = true)
    fun getPendingTransactionsByLastBlock(
        lastBlock: Block,
        depth: Int = Application.MAX_REFERENCED_BLOCK_DEPTH
    ): List<Transaction> {

        val libHash = if (lastBlock.height > 0) {
            lastBlock.libHash
        } else {
            lastBlock.hash
        }

        val lib = blockService.getByHash(libHash)
        val minHeight = lib.height - depth

        // Not finalized blocks, current branch
        val blocksAfterLIB = getBlocksRange(lib, lastBlock).mapNotNull { it.id }

        // Not finalized tx ids, current branch
        val txsAfterLIB = if (blocksAfterLIB.isNotEmpty()) {
            transactionRepository.getTransactionsByBlockIds(blocksAfterLIB)
        } else {
            listOf()
        }

        val pendingTransactions = transactionRepository.getPendingTransactionsBySpace(lastBlock.spaceId)

        val minTimestamp = lastBlock.timestamp - MAX_TRANSACTION_AGE

        return pendingTransactions.subtract(txsAfterLIB).filter { tx ->
            // TODO try to optimize it to avoid repeated calls to dbs
            val referencedBlock = blockService.findByHash(tx.referencedBlockHash)
            tx.timestamp >= minTimestamp && referencedBlock != null && referencedBlock.height > minHeight
        }
    }
}