package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.MAX_DELTA_SIZE
import org.bloqly.machine.component.TransactionProcessor
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.PropertyContext
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType.CREATE
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
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
    private val spaceRepository: SpaceRepository,
    private val transactionProcessor: TransactionProcessor,
    private val voteRepository: VoteRepository,
    private val propertyService: PropertyService,
    private val contractService: ContractService
) {

    fun newBlock(
        spaceId: String,
        height: Long,
        weight: Long,
        diff: Int,
        timestamp: Long,
        parentId: String,
        producerId: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray
    ): Block {

        val round = TimeUtils.getCurrentRound()

        return accountRepository
            .findById(producerId)
            .filter { it.hasKey() }
            .map { proposer ->

                val dataToSign = CryptoUtils.hash(
                    arrayOf(
                        spaceId.toByteArray(),
                        EncodingUtils.longToBytes(height),
                        EncodingUtils.longToBytes(weight),
                        EncodingUtils.intToBytes(diff),
                        EncodingUtils.longToBytes(round),
                        EncodingUtils.longToBytes(timestamp),
                        parentId.toByteArray(),
                        producerId.toByteArray(),
                        txHash ?: ByteArray(0),
                        validatorTxHash
                    )
                )

                val privateKey = proposer.privateKey.decode16()
                val signature = CryptoUtils.sign(privateKey, dataToSign)
                val blockHash = CryptoUtils.hash(signature)
                val blockId = blockHash.encode16()

                Block(
                    id = blockId,
                    spaceId = spaceId,
                    height = height,
                    weight = weight,
                    diff = diff,
                    round = round,
                    timestamp = timestamp,
                    parentId = parentId,
                    proposerId = producerId,
                    txHash = txHash,
                    validatorTxHash = validatorTxHash,
                    signature = signature
                )
            }
            .orElseThrow {
                IllegalArgumentException("Could not create block in behalf of producer $producerId")
            }
    }

    fun getLastBlockForSpace(space: String): Block {
        return blockRepository.getLastBlock(space)
    }

    fun exportFirst(spaceId: String): String {

        val firstBlock = blockRepository.findGenesisBlockBySpaceId(spaceId)

        val transactions = transactionRepository.findByContainingBlockId(firstBlock.id)

        val genesis = Genesis(
            block = firstBlock.toVO(),
            transactions = transactions.map { it.toVO() }
        )

        val json = ObjectUtils.writeValueAsString(genesis)

        return json.toByteArray().encode16()
    }

    fun importFirst(genesisString: String) {

        val now = Instant.now()

        val json = genesisString.decode16()

        val genesis = ObjectUtils.readValue(json, Genesis::class.java)

        ensureSpaceEmpty(genesis.block.spaceId)

        val block = genesis.block.toModel()

        spaceRepository.save(
            Space(
                id = block.spaceId,
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

        val transaction = genesis.transactions.first()

        val contractBody = transaction.toModel().value

        val contractBodyHash = CryptoUtils.hash(contractBody).encode16()

        require(block.parentId == contractBodyHash) {
            "Genesis block parentId be set to the genesis parameters hash, found ${block.parentId} instead."
        }

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

        val propertyContext = PropertyContext(propertyService, contractService)

        transactions.forEach {
            transactionProcessor.processTransaction(it, propertyContext)
        }

        transactionRepository.saveAll(transactions)
    }

    private fun validateGenesisTransaction(transaction: Transaction, block: Block, now: Instant) {
        require(CryptoUtils.verifyTransaction(transaction)) {
            "Transaction in genesis is invalid."
        }

        require(transaction.containingBlockId == block.id) {
            "Transaction has invalid containingBlockId."
        }

        require(transaction.referencedBlockId == block.id) {
            "Transaction has invalid referencedBlockId."
        }

        require(transaction.spaceId == block.spaceId) {
            "Transaction spaceId ${transaction.spaceId} should be the same as block spaceId ${block.spaceId}."
        }

        require(transaction.destination == Application.DEFAULT_SELF) {
            "Genesis transaction destination should be $DEFAULT_SELF, not ${transaction.destination}."
        }

        require(transaction.self == Application.DEFAULT_SELF) {
            "Genesis transaction 'self' should be $DEFAULT_SELF, not ${transaction.self}."
        }

        require(transaction.transactionType == CREATE) {
            "Genesis transaction type should be $CREATE, not ${transaction.transactionType}"
        }

        require(transaction.timestamp < now.toEpochMilli()) {
            "We don't accept transactions from the future, timestamp: ${transaction.timestamp}"
        }
    }

    fun ensureSpaceEmpty(space: String) {

        require(!blockRepository.existsBySpaceId(space)) {
            "Blockchain already initialized for spaceId '$space'"
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
            val votes = voteRepository.findByBlockId(block.parentId)

            BlockData(block, transactions, votes)
        })
    }
}