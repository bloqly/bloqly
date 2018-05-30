package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.vo.GenesisVO
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class GenesisService(
    private val objectMapper: ObjectMapper,
    private val transactionRepository: TransactionRepository,
    private val blockRepository: BlockRepository
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

        val genesis = objectMapper.readValue(genesisString, GenesisVO::class.java)

        val block = genesis.block.toModel()

        val transactions = genesis.transactions.map { it.toModel() }

        require(transactions.size == 1)
        { "Genesis block can contain only 1 transaction." }

        val transaction = transactions.first()

        require(CryptoUtils.isTransactionValid(transaction))
        { "Transaction in genesis is invalid." }

        //require(transaction.containingBlockId == block.id)
        //{ "Transaction has invalid containingBlockId" }

        require(transaction.referencedBlockId == block.id)
        { "Transaction has invalid referencedBlockId" }

        blockRepository.save(block)

        transactionRepository.saveAll(transactions)
    }
}