package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.decode64
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.Genesis
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GenesisService(
    private val blockRepository: BlockRepository,
    private val transactionRepository: TransactionRepository,
    private val spaceRepository: SpaceRepository,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val transactionProcessor: TransactionProcessor,
    private val blockService: BlockService
) {

    @Transactional(readOnly = true)
    fun exportFirst(spaceId: String): String {

        val firstBlock = blockRepository.findGenesisBlockBySpaceId(spaceId)

        val transactions = firstBlock.transactions.map { it.toVO() }

        val genesis = Genesis(
            block = firstBlock.toVO(),
            transactions = transactions
        )

        val json = ObjectUtils.writeValueAsString(genesis)

        return json.toByteArray().encode16()
    }

    @Transactional
    fun importFirst(genesisString: String) {

        val now = Instant.now()

        val json = genesisString.decode16()

        val genesis = ObjectUtils.readValue(json, Genesis::class.java)

        blockService.ensureSpaceEmpty(genesis.block.spaceId)

        val block = genesis.block.toModel()

        spaceRepository.save(
            Space(
                id = block.spaceId,
                creatorId = genesis.block.producerId
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

        val contractBodyHash = CryptoUtils.hash(contractBody.decode64()).encode16()

        require(block.parentHash == contractBodyHash) {
            "Genesis block parentHash be set to the genesis parameters hash, found ${block.parentHash} instead."
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

        transactions.forEach { tx ->
            val result = transactionProcessor.processTransaction(tx, propertyContext)
            require(result.isOK()) {
                "Could not process transaction ${tx.toVO()}"
            }
        }

        propertyContext.commit()

        transactionRepository.saveAll(transactions)
    }

    private fun validateGenesisTransaction(transaction: Transaction, block: Block, now: Instant) {
        require(CryptoUtils.verifyTransaction(transaction)) {
            "Transaction in genesis is invalid."
        }

        require(transaction.referencedBlockHash.isEmpty()) {
            "Transaction has invalid referencedBlockHash."
        }

        require(transaction.spaceId == block.spaceId) {
            "Transaction spaceId ${transaction.spaceId} should be the same as block spaceId ${block.spaceId}."
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
}