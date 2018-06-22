package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val blockCandidateRepository: BlockCandidateRepository,
    // TODO make object mapper a static thing
    private val objectMapper: ObjectMapper
) {

    fun getVote(space: Space, validator: Account): Vote {

        val lastBlock = blockRepository.getLastBlock(space.id)

        val newHeight = lastBlock.height

        // did I vote for H + 1?
        val newHeightVoteId = VoteId(
            validatorId = validator.id,
            spaceId = space.id,
            height = newHeight
        )

        return voteRepository.findById(newHeightVoteId).orElseGet {
            // is there a BC for new height?
            val blockCandidate = blockCandidateRepository.getBlockCandidate(space.id, newHeight, validator.id)

            if (blockCandidate != null) {
                // Voting for a BC with new height
                val block = getBlock(blockCandidate)
                createVote(validator, block, newHeightVoteId)
            } else {
                // nothing found, voting for the LIB
                val currentHeightVoteId = newHeightVoteId.copy(height = lastBlock.height)
                createVote(validator, lastBlock, currentHeightVoteId)
            }
        }
    }

    private fun getBlock(blockCandidate: BlockCandidate): Block {
        return objectMapper.readValue(blockCandidate.data, BlockData::class.java).block.toModel()
    }

    private fun createVote(validator: Account, block: Block, voteId: VoteId): Vote {

        val timestamp = Instant.now().toEpochMilli()

        val dataToSign = Bytes.concat(
            validator.id.toByteArray(),
            block.spaceId.toByteArray(),
            EncodingUtils.longToBytes(block.height),
            block.id.toByteArray(),
            EncodingUtils.longToBytes(timestamp)
        )

        val dataHash = CryptoUtils.digest(dataToSign)
        val privateKey = decodeFromString16(validator.privateKey)
        val signature = CryptoUtils.sign(privateKey, dataHash)

        val vote = Vote(
            id = voteId,
            blockId = block.id,
            timestamp = timestamp,
            signature = signature,
            publicKey = validator.publicKey!!
        )

        return voteRepository.save(vote)
    }

    fun processVote(vote: Vote) {
        require(CryptoUtils.verifyVote(vote)) {
            "Could not verify vote."
        }

        // already received
        if (voteRepository.existsById(vote.id)) {
            return
        }

        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }

        val votedBlock = blockRepository.findById(vote.blockId).orElseThrow()
        val lastBlock = blockRepository.getLastBlock(votedBlock.spaceId)

        require(votedBlock == lastBlock) {
            "Vote is for block ${votedBlock.id}, last block for space ${votedBlock.spaceId} is ${lastBlock.id}."
        }

        voteRepository.save(vote)
    }

    fun save(vote: Vote) {
        require(CryptoUtils.verifyVote(vote)) {
            "Could not verify vote $vote"
        }

        voteRepository.save(vote)
    }
}