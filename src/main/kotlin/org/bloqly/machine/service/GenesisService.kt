package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.model.TransactionType.CREATE
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.vo.GenesisVO
import org.springframework.stereotype.Service
import java.time.Instant
import javax.transaction.Transactional

@Service
@Transactional
class GenesisService(
    private val objectMapper: ObjectMapper,
    private val transactionRepository: TransactionRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository
) {

    fun exportGenesis(space: String): String {

        val firstBlock = blockRepository.findGenesisBlockBySpace(space)

        val transactions = transactionRepository.findByContainingBlockId(firstBlock.id)

        val genesis = GenesisVO(
            block = firstBlock.toVO(),
            transactions = transactions.map { it.toVO() }
        )

        return objectMapper.writeValueAsString(genesis)
    }

    fun importGenesis(genesisString: String) {

        val now = Instant.now().toEpochMilli()

        val genesis = objectMapper.readValue(genesisString, GenesisVO::class.java)

        val block = genesis.block.toModel()

        // TODO
        //require(CryptoUtils.isBlockValid())

        require(!spaceRepository.existsById(block.space)) {
            "Space ${block.space} already exists."
        }

        require(block.height == 0L) {
            "Genesis block should have height 0, found ${block.height} instead."
        }

        require(block.timestamp < now) {
            "We don't accept blocks from the future, timestamp: ${block.timestamp}."
        }

        require(block.parentHash.isEmpty()) {
            "Genesis block parentHash should be empty, found ${block.parentHash} instead."
        }

        val transactions = genesis.transactions.map { it.toModel() }

        require(transactions.size == 1) {
            "Genesis block can contain only 1 transaction."
        }

        val transaction = transactions.first()

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

        require(transaction.destination == DEFAULT_SELF) {
            "Genesis transaction destination should be $DEFAULT_SELF, not ${transaction.destination}."
        }

        require(transaction.self == DEFAULT_SELF) {
            "Genesis transaction 'self' should be $DEFAULT_SELF, not ${transaction.self}."
        }

        require(transaction.transactionType == CREATE) {
            "Genesis transaction type shpould be $CREATE, not ${transaction.transactionType}"
        }

        require(transaction.timestamp < now) {
            "We don't accept transactions from the future, timestamp: ${transaction.timestamp}"
        }

        blockRepository.save(block)

        transactionRepository.saveAll(transactions)
    }
}