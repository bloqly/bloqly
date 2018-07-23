package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
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
    private val accountService: AccountService
) {

    fun getVote(space: Space, validator: Account, passphrase: String): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        val newHeight = lastBlock.height + 1

        return voteRepository.findBySpaceIdAndValidatorAndHeight(space.id, validator, newHeight)
            ?: createVote(validator, passphrase, lastBlock)
    }

    private fun createVote(
        validator: Account,
        passphrase: String,
        block: Block
    ): Vote {

        val timestamp = Instant.now().toEpochMilli()

        val vote = Vote(
            validator = validator,
            blockHash = block.hash,
            height = block.height + 1,
            spaceId = block.spaceId,
            timestamp = timestamp
        )

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(validator.privateKeyEncoded, passphrase),
            CryptoUtils.hash(vote)
        )

        return voteRepository.save(
            vote.copy(signature = signature)
        )
    }

    fun validateAndSave(vote: Vote): Vote {

        requireVoteValid(vote)

        val validator = accountService.getAccountByPublicKey(vote.validator.publicKey!!)

        voteRepository.findByValidatorAndSpaceIdAndHeight(validator, vote.spaceId, vote.height)?.let {
            return it
        }

        return voteRepository.save(vote)
    }

    fun requireVoteValid(vote: Vote) {
        // TODO create a log file where log full stack traces

        val validator = vote.validator

        require(CryptoUtils.verifyVote(vote, validator.publicKey.decode16())) {
            "Could not verify vote."
        }

        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }
    }
}