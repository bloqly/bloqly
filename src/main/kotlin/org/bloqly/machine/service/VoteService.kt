package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val accountRepository: AccountRepository
) {

    fun getVote(space: Space, validator: Account): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        val newHeight = lastBlock.height + 1

        return voteRepository.findBySpaceIdAndValidatorIdAndHeight(space.id, validator.id, newHeight)
            ?: createVote(validator, lastBlock)
    }

    private fun createVote(
        validator: Account,
        block: Block
    ): Vote {

        val timestamp = Instant.now().toEpochMilli()

        val vote = Vote(
            validatorId = validator.id,
            blockHash = block.hash,
            height = block.height + 1,
            spaceId = block.spaceId,
            timestamp = timestamp
        )

        val signature = CryptoUtils.sign(
            validator.privateKey.decode16(),
            CryptoUtils.hash(vote)
        )

        return voteRepository.save(
            vote.copy(signature = signature)
        )
    }

    fun validateAndSave(vote: Vote) {

        verifyVote(vote)

        if (voteRepository.existsByValidatorIdAndSpaceIdAndHeight(vote.validatorId, vote.spaceId, vote.height)) {
            return
        }

        voteRepository.save(vote)
    }

    fun verifyVote(vote: Vote) {
        // TODO create a log file where log full stack traces

        val validator = accountRepository.findById(vote.validatorId).orElseThrow()

        require(CryptoUtils.verifyVote(vote, validator.publicKey.decode16())) {
            "Could not verify vote."
        }

        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }
    }
}