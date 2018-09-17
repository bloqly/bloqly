package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.helper.CryptoHelper
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.FinalizedTransaction
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.FinalizedTransactionRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.PropertyService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.fromHex
import org.bloqly.machine.util.toHex
import org.bloqly.machine.vo.genesis.Genesis
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GenesisService(
    private val blockRepository: BlockRepository,
    private val transactionRepository: TransactionRepository,
    private val spaceService: SpaceService,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val transactionProcessor: TransactionProcessor,
    private val blockService: BlockService,
    private val finalizedTransactionRepository: FinalizedTransactionRepository
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

        return json.toByteArray().toHex()
    }

    @Transactional
    fun importFirst(genesisString: String) {

        val now = Instant.now()

        val json = genesisString.fromHex()

        val genesis = ObjectUtils.readValue(json, Genesis::class.java)

        blockService.ensureSpaceEmpty(genesis.block.spaceId)

        val block = genesis.block.toModel()

        spaceService.save(
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
        require(block.height == 0L) {
            "Genesis block should have height 0, found ${block.height} instead."
        }

        require(block.timestamp < now.toEpochMilli()) {
            "We don't accept blocks from the future, timestamp: ${block.timestamp}."
        }

        val transaction = genesis.transactions.first()

        val contractBodyValue = transaction.toModel().value.first()
        val contractBodyEncoded = contractBodyValue.toValue() as String
        val contractBody = String(contractBodyEncoded.fromHex())

        val contractBodyHash = CryptoUtils.hash(contractBody).toHex()

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

        val transaction = transactions.first()

        validateGenesisTransaction(transaction, block, now)

        val propertyContext = PropertyContext(propertyService, contractService)

        val result = transactionProcessor.processTransaction(transaction, propertyContext)
        require(result.isOK()) {
            "Could not process transaction ${transaction.toVO()}"
        }

        propertyContext.commit()

        transactionRepository.save(transaction).let { tx ->
            finalizedTransactionRepository.save(
                FinalizedTransaction(
                    transaction = tx,
                    block = block
                )
            )
        }
    }

    private fun validateGenesisTransaction(transaction: Transaction, block: Block, now: Instant) {
        require(CryptoHelper.verifyTransaction(transaction)) {
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