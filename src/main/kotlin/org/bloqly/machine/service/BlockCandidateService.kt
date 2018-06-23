package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
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
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository
) {

    fun validateAndSave(blockData: BlockData) {

        if (!validateProposal(blockData)) {
            // TODO it is better to throw things here
            return
        }

        save(blockData)
    }

    fun save(blockData: BlockData) {
        val block = blockData.block

        val blockCandidateId = BlockCandidateId(
            spaceId = block.spaceId,
            height = block.height,
            round = block.round,
            proposerId = block.proposerId
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
                id = blockCandidateId,
                data = objectMapper.writeValueAsString(blockData),
                blockId = blockData.block.id,
                timeReceived = Instant.now().toEpochMilli()
            )
        )
    }

    private fun validateProposal(blockData: BlockData): Boolean {

        val round = TimeUtils.getCurrentRound()

        val block = blockData.block

        val roundOK = block.round == round

        val votes = blockData.votes

        val lastBlock = blockRepository.getLastBlock(block.spaceId)

        val votesForLastBlock = votes.all { it.blockId == lastBlock.id }

        val votesVerified = votes.all { CryptoUtils.verifyVote(it.toModel()) }

        val transactions = blockData.transactions

        val referencedBlockIdsOK = transactions.all { it.referencedBlockId == lastBlock.id }

        val transactionsVerifiedOK = transactions.all { CryptoUtils.verifyTransaction(it.toModel()) }

        return roundOK && votesForLastBlock && votesVerified &&
            referencedBlockIdsOK && transactionsVerifiedOK
    }

    fun getBlockCandidate(space: Space, height: Long, producerId: String): BlockData? {
        val blockCandidate = blockCandidateRepository
            .getBlockCandidate(space.id, height, producerId)

        return blockCandidate?.let {
            objectMapper.readValue(blockCandidate.data, BlockData::class.java)
        }
    }

    fun getVotedBlockCandidate(space: Space, height: Long): BlockData? {
        val quorum = propertyRepository.getQuorumBySpace(space)
        return blockCandidateRepository.findBySpaceIdAndHeight(space.id, height)
            .map { blockCandidate ->
                objectMapper.readValue(blockCandidate.data, BlockData::class.java)
            }
            // TODO check if there is several voted block candidates for the same height
            // as this would break consensus rules
            .firstOrNull { blockData ->
                val votes = voteRepository.findByBlockId(blockData.block.id)

                votes.size >= quorum
            }
    }
}