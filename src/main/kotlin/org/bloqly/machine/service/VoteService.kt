package org.bloqly.machine.service

import com.google.common.primitives.Bytes
import org.bloqly.machine.component.CryptoService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
@Transactional
class VoteService(

    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val cryptoService: CryptoService

) {

    fun createVote(space: String, validator: Account): Vote {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space)
        val timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()

        val dataToSign = Bytes.concat(

                validator.id.toByteArray(),
                space.toByteArray(),
                EncodingUtils.longToBytes(lastBlock.height),
                lastBlock.id.toByteArray(),
                EncodingUtils.longToBytes(timestamp)
        )

        val dataHash = cryptoService.digest(dataToSign)
        val privateKey = decodeFromString(validator.privateKey)
        val signature = cryptoService.sign(privateKey, dataHash)

        val vote = Vote(
                VoteId(
                        validatorId = validator.id,
                        space = space,
                        height = lastBlock.height
                ),

                blockId = lastBlock.id,
                timestamp = timestamp,
                signature = signature
        )

        return voteRepository.save(vote)
    }
}