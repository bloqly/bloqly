package org.bloqly.machine.service

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.repository.BlockRepository
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
    private val blockRepository: BlockRepository
) {

    fun createVote(space: String, validator: Account): Vote {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space)
        val timestamp = Instant.now().toEpochMilli()

        val dataToSign = Bytes.concat(

            validator.id.toByteArray(),
            space.toByteArray(),
            EncodingUtils.longToBytes(lastBlock.height),
            lastBlock.id.toByteArray(),
            EncodingUtils.longToBytes(timestamp)
        )

        val dataHash = CryptoUtils.digest(dataToSign)
        val privateKey = decodeFromString16(validator.privateKey)
        val signature = CryptoUtils.sign(privateKey, dataHash)

        val vote = Vote(
            VoteId(
                validatorId = validator.id,
                space = space,
                height = lastBlock.height
            ),

            blockId = lastBlock.id,
            timestamp = timestamp,
            signature = signature,
            publicKey = validator.publicKey!!
        )

        return voteRepository.save(vote)
    }
}