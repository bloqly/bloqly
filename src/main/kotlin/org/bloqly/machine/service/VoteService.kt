package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val blockRepository: BlockRepository
) {

    private val log = LoggerFactory.getLogger(VoteService::class.simpleName)

    @Transactional
    fun findOrCreateVote(space: Space, validator: Account, passphrase: String): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        return if (
            voteRepository.existsByHeight(
                space.id,
                validator.publicKey,
                lastBlock.height
            ) // did I vote for this height
            || lastBlock.producerId == validator.accountId  // don't vote for the own block

        ) {
            // if so, I just will send my best vote
            voteRepository.findBestVote(space.id, validator.publicKey)
        } else {
            createVote(validator, passphrase, lastBlock)
        }
    }

    @Transactional
    internal fun createVote(
        validator: Account,
        passphrase: String,
        block: Block
    ): Vote {
        return voteRepository.save(
            newVote(validator, passphrase, block)
        )
    }

    fun newVote(
        validator: Account,
        passphrase: String,
        block: Block
    ): Vote {
        val vote = Vote(
            publicKey = validator.publicKey,
            blockHash = block.hash,
            height = block.height,
            spaceId = block.spaceId,
            timestamp = TimeUtils.getCurrentTime()
        )

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(validator.privateKeyEncoded, passphrase),
            CryptoUtils.hash(vote)
        )

        return vote.copy(signature = signature)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun verifyAndSave(vote: Vote): Vote {

        requireVoteValid(vote)

        return voteRepository.save(vote)
    }

    @Transactional
    fun findVote(publicKey: String, blockHash: String): Vote? =
        voteRepository.findByPublicKeyAndBlockHash(publicKey, blockHash)

    fun requireVoteValid(vote: Vote) {

        require(CryptoUtils.verifyVote(vote, vote.publicKey.decode16())) {
            "Could not verify vote."
        }

        // TODO get timestamp from last block maybe?
        val now = Instant.now().toEpochMilli()

        require(vote.timestamp < now) {
            "Can not accept vote form the future."
        }
    }

    @Transactional
    fun isAcceptable(vote: Vote): Boolean {

        if (voteRepository.existsBySpaceIdAndPublicKeyAndHeight(vote.spaceId, vote.publicKey, vote.height)) {
            log.warn("Vote with validator and height already exists ${vote.toVO()}")
            return false
        }

        if (voteRepository.existsByPublicKeyAndBlockHash(vote.publicKey, vote.blockHash)) {
            log.warn("Validator has already voted for this block ${vote.toVO()}")
        }

        return true
    }
}