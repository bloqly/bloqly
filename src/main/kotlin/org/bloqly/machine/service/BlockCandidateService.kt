package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class BlockCandidateService(
    private val blockCandidateRepository: BlockCandidateRepository,
    private val objectMapper: ObjectMapper,
    private val transactionRepository: TransactionRepository,
    private val voteRepository: VoteRepository
) {

    fun saveAll(blocks: List<BlockData>) {

        blocks.forEach { blockData ->

            val transactions = blockData.transactions.map { it.toModel() }
            val votes = blockData.votes.map { it.toModel() }

            transactionRepository.saveAll(transactions)
            voteRepository.saveAll(votes)

            val blockCandidate = BlockCandidate(
                id = BlockCandidateId(
                    space = blockData.block.space,
                    height = blockData.block.height,
                    proposerId = blockData.block.proposerId
                ),
                data = objectMapper.writeValueAsString(blockData),
                timeReceived = Instant.now().toEpochMilli()
            )

            blockCandidateRepository.save(blockCandidate)
        }
    }

    fun getBlockCandidate(space: String, height: Long, proposerId: String): BlockData? {
        return blockCandidateRepository
            .findById(BlockCandidateId(space, height, proposerId))
            .map { blockCandidate ->
                objectMapper.readValue(blockCandidate.data, BlockData::class.java)
            }
            .orElse(null)
    }
}