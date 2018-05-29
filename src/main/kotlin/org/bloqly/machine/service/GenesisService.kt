package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.TransactionRepository
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
}