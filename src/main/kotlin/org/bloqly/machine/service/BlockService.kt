package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.MAX_DELTA_SIZE
import org.bloqly.machine.component.TransactionProcessor
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.Delta
import org.bloqly.machine.vo.Genesis
import org.springframework.stereotype.Service
import java.time.Instant
import javax.transaction.Transactional
import kotlin.math.min

@Service
@Transactional
class BlockService(
    private val accountRepository: AccountRepository,
    private val blockRepository: BlockRepository,
    private val transactionRepository: TransactionRepository,
    private val objectMapper: ObjectMapper,
    private val spaceRepository: SpaceRepository,
    private val transactionProcessor: TransactionProcessor,
    private val voteRepository: VoteRepository
) {

    fun newBlock(
        space: String,
        height: Long,
        timestamp: Long,
        parentHash: String,
        proposerId: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray
    ): Block {

        val accountOpt = accountRepository
            .findById(proposerId)
            .filter { it.privateKey != null }

        return accountOpt.map { proposer ->

            val dataToSign = CryptoUtils.digest(
                arrayOf(
                    space.toByteArray(),
                    EncodingUtils.longToBytes(height),
                    EncodingUtils.longToBytes(timestamp),
                    parentHash.toByteArray(),
                    proposerId.toByteArray(),
                    txHash ?: ByteArray(0),
                    validatorTxHash
                )
            )

            val privateKey = decodeFromString16(proposer.privateKey)
            val signature = CryptoUtils.sign(privateKey, dataToSign)
            val blockHash = CryptoUtils.digest(signature)
            val blockId = EncodingUtils.encodeToString16(blockHash)

            Block(
                id = blockId,
                space = space,
                height = height,
                timestamp = timestamp,
                parentHash = parentHash,
                proposerId = proposerId,
                txHash = txHash,
                validatorTxHash = validatorTxHash,
                signature = signature
            )
        }.orElseThrow()
    }

    fun getLastBlockForSpace(space: String): Block {
        return blockRepository.getLastBlock(space)
    }

    fun exportFirst(space: String): String {

        val firstBlock = blockRepository.findGenesisBlockBySpace(space)

        val transactions = transactionRepository.findByContainingBlockId(firstBlock.id)

        val genesis = Genesis(
            block = firstBlock.toVO(),
            transactions = transactions.map { it.toVO() }
        )

        return objectMapper.writeValueAsString(genesis)
    }

    fun importFirst(genesisString: String) {

        val now = Instant.now()

        val genesis = objectMapper.readValue(genesisString, Genesis::class.java)

        ensureSpaceEmpty(genesis.block.space)

        val block = genesis.block.toModel()

        spaceRepository.save(
            Space(
                id = block.space,
                creatorId = genesis.block.proposerId
            )
        )

        processImportGenesisBlock(genesis, block, now)

        processImportGenesisTransactions(genesis, block, now)
    }

    private fun processImportGenesisBlock(
        genesis: Genesis,
        block: Block,
        now: Instant
    ) {
        // TODO
        // require(CryptoUtils.isBlockValid())

        require(block.height == 0L) {
            "Genesis block should have height 0, found ${block.height} instead."
        }

        require(block.timestamp < now.toEpochMilli()) {
            "We don't accept blocks from the future, timestamp: ${block.timestamp}."
        }

        /* TODO
        require(block.parentHash == EncodingUtils.encodeToString16(genesisHash)) {
            "Genesis block parentHash be set to the genesis parameters hash, found ${block.parentHash} instead."
        }
        */

        blockRepository.save(block)
    }

    private fun processImportGenesisTransactions(
        genesis: Genesis,
        block: Block,
        now: Instant
    ) {
        val transactions = genesis.transactions.map { it.toModel() }

        require(transactions.size == 1) {
            "Genesis block can contain only 1 transaction."
        }

        validateGenesisTransaction(transactions.first(), block, now)

        transactions.forEach { transactionProcessor.processTransaction(it) }

        transactionRepository.saveAll(transactions)
    }

    private fun validateGenesisTransaction(transaction: Transaction, block: Block, now: Instant) {
        require(CryptoUtils.isTransactionValid(transaction)) {
            "Transaction in genesis is invalid."
        }

        require(transaction.containingBlockId == block.id) {
            "Transaction has invalid containingBlockId."
        }

        require(transaction.referencedBlockId == block.id) {
            "Transaction has invalid referencedBlockId."
        }

        require(transaction.space == block.space) {
            "Transaction space ${transaction.space} should be the same as block space ${block.space}."
        }

        require(transaction.destination == Application.DEFAULT_SELF) {
            "Genesis transaction destination should be ${Application.DEFAULT_SELF}, not ${transaction.destination}."
        }

        require(transaction.self == Application.DEFAULT_SELF) {
            "Genesis transaction 'self' should be ${Application.DEFAULT_SELF}, not ${transaction.self}."
        }

        require(transaction.transactionType == TransactionType.CREATE) {
            "Genesis transaction type should be ${TransactionType.CREATE}, not ${transaction.transactionType}"
        }

        require(transaction.timestamp < now.toEpochMilli()) {
            "We don't accept transactions from the future, timestamp: ${transaction.timestamp}"
        }
    }

    fun ensureSpaceEmpty(space: String) {

        require(!blockRepository.existsBySpace(space)) {
            "Blockchain already initialized for space '$space'"
        }

        require(!spaceRepository.existsById(space)) {
            "Space '$space' already exists"
        }
    }

    fun getBlockDataList(delta: Delta): BlockDataList {

        val startHeight = delta.localHeight
        val endHeight = min(delta.remoteHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(delta.spaceId, startHeight, endHeight)

        return BlockDataList(blocks.map { block ->

            val transactions = transactionRepository.findByContainingBlockId(block.id)
            val votes = voteRepository.findByBlockId(block.parentHash)

            BlockData(block, transactions, votes)
        })
    }
}