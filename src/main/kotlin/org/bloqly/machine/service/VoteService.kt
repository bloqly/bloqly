package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.decode16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository,
    private val accountRepository: AccountRepository
) {

    @Transactional
    fun findOrCreateVote(space: Space, validator: Account, passphrase: String): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        return voteRepository.findByValidatorAndBlockHash(validator, lastBlock.hash)
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun verifyAndSave(vote: Vote): Vote {

        requireVoteValid(vote)

        return voteRepository.save(vote)
    }

    @Transactional
    fun findVote(publicKey: String, blockHash: String): Vote? {

        val accountId = EncodingUtils.hashAndEncode16(publicKey.decode16())

        val validator = accountRepository.findByAccountId(accountId)

        return validator?.let {
            voteRepository.findByValidatorAndBlockHash(validator, blockHash)
        }
    }

    fun requireVoteValid(vote: Vote) {

        val validator = vote.validator

        require(CryptoUtils.verifyVote(vote, validator.publicKey.decode16())) {
            "Could not verify vote."
        }

        // TODO get timestamp from last block maybe?
        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }
    }
}