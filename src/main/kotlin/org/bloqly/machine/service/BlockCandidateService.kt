package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.PropertyRepository
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
    private val voteRepository: VoteRepository,
    private val propertyRepository: PropertyRepository
) {

    fun save(blockData: BlockData) {

        val blockCandidateId = BlockCandidateId(
            space = blockData.block.spaceId,
            height = blockData.block.height,
            proposerId = blockData.block.proposerId
        )

        if (blockCandidateRepository.existsById(blockCandidateId)) {
            return
        }

        val transactions = blockData.transactions.map { it.toModel() }
        val votes = blockData.votes.map { it.toModel() }

        transactionRepository.saveAll(transactions)
        voteRepository.saveAll(votes)

        blockCandidateRepository.save(
            BlockCandidate(
                id = BlockCandidateId(
                    space = blockData.block.spaceId,
                    height = blockData.block.height,
                    proposerId = blockData.block.proposerId
                ),
                data = objectMapper.writeValueAsString(blockData),
                timeReceived = Instant.now().toEpochMilli()
            )
        )
    }

    fun getBlockCandidate(space: Space, height: Long, proposerId: String): BlockData? {
        val quorum = propertyRepository.getQuorumBySpace(space)
        return blockCandidateRepository
            .findById(BlockCandidateId(space.id, height, proposerId))
            .map { blockCandidate ->
                objectMapper.readValue(blockCandidate.data, BlockData::class.java)
            }
            .filter { it.votes.size >= quorum }
            .orElse(null)
    }
}