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
import org.bloqly.machine.repository.PropertyRepository
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
    private val objectMapper: ObjectMapper,
    private val propertyRepository: PropertyRepository
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
                val block = getBlockData(blockCandidate).block.toModel()
                createVote(validator, block, newHeightVoteId)
            } else {
                // nothing found, voting for the LIB
                val currentHeightVoteId = newHeightVoteId.copy(height = lastBlock.height)
                createVote(validator, lastBlock, currentHeightVoteId)
            }
        }
    }

    private fun getBlockData(blockCandidate: BlockCandidate): BlockData {
        return objectMapper.readValue(blockCandidate.data, BlockData::class.java)
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

    fun processVote(vote: Vote): BlockData? {

        validateAndSave(vote)

        val votedBlockOpt = blockRepository.findById(vote.blockId)

        if (!votedBlockOpt.isPresent) {
            return null
        }

        // TODO implement security consensus rules check
        // TODO move it out to some kind of loop

        val votedBlock = votedBlockOpt.get()

        val lastBlock = blockRepository.getLastBlock(votedBlock.spaceId)

        val newHeight = lastBlock.height + 1

        val blockCandidate = blockCandidateRepository.getBlockCandidateByBlockId(vote.blockId)

        return blockCandidate?.let {

            if (it.id.height != newHeight) {
                return null
            }

            val blockData = getBlockData(blockCandidate)

            require(blockData.block.parentHash == lastBlock.id)

            blockData
        }
    }

    fun validateAndSave(vote: Vote) {

        validateVote(vote)

        // already received
        if (voteRepository.existsById(vote.id)) {
            return
        }

        voteRepository.save(vote)
    }

    private fun validateVote(vote: Vote) {
        // TODO create a log file where log full stack traces
        require(CryptoUtils.verifyVote(vote)) {
            "Could not verify vote."
        }

        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }
    }

    // TODO move it to property repository or elsewhere
    private fun hasQuorum(blockData: BlockData): Boolean {

        val quorum = propertyRepository.getQuorumBySpaceId(blockData.block.spaceId)

        return blockData.votes.size >= quorum
    }
}