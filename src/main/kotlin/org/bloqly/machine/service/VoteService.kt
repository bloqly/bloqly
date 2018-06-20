package org.bloqly.machine.service

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.RoundRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val roundRepository: RoundRepository
) {

    fun createVote(space: String, validator: Account): Vote {

        val lastBlock = blockRepository.getLastBlock(space)

        val voteId = VoteId(
            validatorId = validator.id,
            space = space,
            height = lastBlock.height
        )

        return voteRepository.findById(voteId).orElseGet {
            val timestamp = Instant.now().toEpochMilli()

            val round = roundRepository.getRound(space)

            val dataToSign = Bytes.concat(
                validator.id.toByteArray(),
                round.producerId.toByteArray(),
                space.toByteArray(),
                EncodingUtils.longToBytes(lastBlock.height),
                EncodingUtils.longToBytes(round.id.round),
                lastBlock.id.toByteArray(),
                EncodingUtils.longToBytes(timestamp)
            )

            val dataHash = CryptoUtils.digest(dataToSign)
            val privateKey = decodeFromString16(validator.privateKey)
            val signature = CryptoUtils.sign(privateKey, dataHash)

            val vote = Vote(
                id = voteId,
                blockId = lastBlock.id,
                round = round.id.round,
                proposerId = round.producerId,
                timestamp = timestamp,
                signature = signature,
                publicKey = validator.publicKey!!
            )

            voteRepository.save(vote)
        }
    }

    fun processVote(vote: Vote) {
        require(CryptoUtils.verifyVote(vote)) {
            "Could not verify vote $vote"
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