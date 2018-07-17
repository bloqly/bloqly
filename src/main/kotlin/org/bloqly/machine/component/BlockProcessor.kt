package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.InvocationResult
import org.bloqly.machine.model.InvocationResultType
import org.bloqly.machine.model.Properties
import org.bloqly.machine.model.PropertyContext
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionOutput
import org.bloqly.machine.model.TransactionOutputId
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.BlockData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private data class TransactionResult(
    val transaction: Transaction,
    val invocationResult: InvocationResult
)

@Service
@Transactional
class BlockProcessor(
    private val transactionService: TransactionService,
    private val voteService: VoteService,
    private val voteRepository: VoteRepository,
    private val blockService: BlockService,
    private val blockRepository: BlockRepository,
    private val transactionProcessor: TransactionProcessor,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val transactionOutputRepository: TransactionOutputRepository
) {

    private val log: Logger = LoggerFactory.getLogger(BlockProcessor::class.simpleName)

    fun processReceivedBlock(blockData: BlockData) {

        try {
            // TODO check LIB for received block
            val block = blockData.block.toModel()
            requireValid(block)

            val propertyContext = PropertyContext(propertyService, contractService)

            val currentLIB = blockService.getLIBForSpace(block.spaceId)

            if (currentLIB.height > 0) {
                var blockToEvaluate = blockRepository.findByParentHash(currentLIB.hash)!!

                while (blockToEvaluate.height < block.height) {

                    evaluateBlock(blockToEvaluate, propertyContext)

                    blockToEvaluate = blockRepository.findByParentHash(blockToEvaluate.hash)!!
                }

                require(blockToEvaluate.hash == block.parentHash)
            }

            val votes = blockData.votes.map { it.toModel() }
            votes.forEach { voteService.requireVoteValid(it) }

            val transactions = blockData.transactions.map { it.toModel() }
            transactions.forEach { tx ->
                val result = transactionProcessor.processTransaction(tx, propertyContext)

                if (result.invocationResultType != InvocationResultType.SUCCESS) {
                    throw RuntimeException("Could not process transaction $tx")
                }
            }

            blockRepository.save(
                block.copy(
                    transactions = transactions,
                    votes = votes
                )
            )

            moveLIB(currentLIB)
        } catch (e: Exception) {
            log.error("Could not process block ${blockData.block.hash} of height ${blockData.block.height}", e)
        }
    }

    private fun evaluateBlock(block: Block, propertyContext: PropertyContext) {

        block.transactions.forEach { tx ->
            val result = transactionProcessor.processTransaction(tx, propertyContext)

            require(result.isOK())

            propertyContext.updatePropertyValues(result.output)
        }
    }

    private fun requireValid(block: Block) {

        require(!blockRepository.existsByHash(block.hash)) {
            "Block hash ${block.hash} already exists"
        }

        require(!blockRepository.existsByHashAndLibHash(block.hash, block.libHash)) {
            "Unique constraint violated (hash, block_hash) : (${block.hash}, ${block.libHash})"
        }

        require(!blockRepository.existsByHashAndParentHash(block.hash, block.parentHash)) {
            "Unique constraint violated (hash, parent_hash) : (${block.hash}, ${block.parentHash})"
        }

        require(!blockRepository.existsBySpaceIdAndProducerIdAndHeight(block.spaceId, block.producerId, block.height)) {
            "Unique constraint violated (space_id, producer_id, height) : (${block.spaceId}, ${block.producerId}, ${block.height})"
        }

        require(!blockRepository.existsBySpaceIdAndProducerIdAndRound(block.spaceId, block.producerId, block.round)) {
            "Unique constraint violated (space_id, producer_id, round) : (${block.spaceId}, ${block.producerId}, ${block.round})"
        }

        require(blockRepository.findByHash(block.parentHash) != null) {
            "No parent found for block hash: ${block.hash}, parent_hash: ${block.parentHash}"
        }
    }

    fun createNextBlock(spaceId: String, producer: Account, round: Long): BlockData {

        blockRepository.findBySpaceIdAndProducerIdAndRound(spaceId, producer.id, round)
            ?.let { return BlockData(it) }

        val txResults = getTransactionsForNextBlock(spaceId)
        val transactions = txResults.map { it.transaction }

        val lastBlock = blockService.getLastBlockForSpace(spaceId)

        val newHeight = lastBlock.height + 1
        val votes = getVotesForBlock(lastBlock.hash)
        val prevVotes = getVotesForBlock(lastBlock.parentHash)

        val diff = votes.minus(prevVotes).size

        val weight = lastBlock.weight + votes.size

        val currentLIB = blockService.getLIBForSpace(spaceId)

        val newBlock = blockService.newBlock(
            spaceId = spaceId,
            height = newHeight,
            weight = weight,
            diff = diff,
            timestamp = Instant.now().toEpochMilli(),
            parentHash = lastBlock.hash,
            producerId = producer.id,
            txHash = CryptoUtils.digestTransactions(transactions),
            validatorTxHash = CryptoUtils.digestVotes(votes),
            round = round,
            transactions = transactions,
            votes = votes
        )

        saveTxOutputs(txResults, newBlock)

        moveLIB(currentLIB)

        return BlockData(blockRepository.save(newBlock))
    }

    private fun moveLIB(currentLIB: Block) {

        val newLIB = blockService.getLIBForSpace(currentLIB.spaceId)

        require(newLIB.height >= currentLIB.height)

        if (newLIB == currentLIB) {
            return
        }

        // apply transaction outputs if LIB moved forward

        var block = currentLIB

        // iterate and apply all transactions from the block next to the previous LIB including NEW_LIB
        // in some situations LIB.height + 1 = NEW_LIB.height
        // but not always
        while (block.height <= newLIB.height) {

            if (block.height > 0) {

                block.transactions.forEach { tx ->

                    val txOutput = transactionOutputRepository
                        .findById(TransactionOutputId(block.hash, tx.hash))
                        .orElseThrow()

                    val properties = ObjectUtils.readProperties(txOutput.output)

                    // TODO add check so that property keys are unique
                    propertyService.updateProperties(properties)
                }
            }

            block = blockRepository.findByParentHash(block.hash)!!
        }

        require(block == newLIB)
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

    private fun getTransactionsForNextBlock(spaceId: String): List<TransactionResult> {
        val transactions = getPendingTransactions(spaceId)

        val propertyContext = PropertyContext(propertyService, contractService)

        // TODO take into account nonce
        return transactions
            .map { tx ->

                val localPropertyContext = propertyContext.copy()

                val result = transactionProcessor.processTransaction(tx, localPropertyContext)

                if (result.isOK()) {
                    propertyContext.merge(localPropertyContext)
                }

                TransactionResult(tx, result)
            }
            .filter { it.invocationResult.isOK() }
    }

    private fun getPendingTransactions(spaceId: String): List<Transaction> {
        return transactionService.getPendingTransactionsBySpace(spaceId, MAX_REFERENCED_BLOCK_DEPTH)
    }

    private fun getVotesForBlock(blockHash: String): List<Vote> {
        return voteRepository.findByBlockHash(blockHash)
    }
}