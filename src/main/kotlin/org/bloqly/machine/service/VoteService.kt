package org.bloqly.machine.service

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val blockCandidateRepository: BlockCandidateRepository
) {

    fun getVote(space: Space, validator: Account): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        val newHeight = lastBlock.height + 1

        val voteId = VoteId(
            validatorId = validator.id,
            spaceId = space.id,
            height = lastBlock.height
        )

        val newHeightVoteId = voteId.copy(height = newHeight)

        return voteRepository.findById(newHeightVoteId).orElseGet {
            // is there a BC for new height?
            val blockCandidate = blockCandidateRepository.getBlockCandidate(space.id, newHeight)

            if (blockCandidate != null) {
                val block = getBlockData(blockCandidate).block.toModel()

                // Voting for a BC with new height
                createVote(
                    validator, block.id, newHeightVoteId
                )
            } else {
                // nothing found, voting for the LIB
                createVote(validator, lastBlock.id, voteId)
            }
        }
    }

    private fun getBlockData(blockCandidate: BlockCandidate): BlockData {
        return ObjectUtils.readValue(blockCandidate.data, BlockData::class.java)
    }

    private fun createVote(
        validator: Account,
        blockId: String,
        voteId: VoteId
    ): Vote {

        val timestamp = Instant.now().toEpochMilli()

        val dataToSign = Bytes.concat(
            validator.id.toByteArray(),
            voteId.spaceId.toByteArray(),
            EncodingUtils.longToBytes(voteId.height),
            voteId.voteType.name.toByteArray(),
            blockId.toByteArray(),
            EncodingUtils.longToBytes(timestamp)
        )

        val dataHash = CryptoUtils.digest(dataToSign)
        val privateKey = validator.privateKey.decode16()
        val signature = CryptoUtils.sign(privateKey, dataHash)

        val vote = Vote(
            id = voteId,
            blockId = blockId,
            timestamp = timestamp,
            signature = signature,
            publicKey = validator.publicKey!!
        )

        return voteRepository.save(vote)
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
}