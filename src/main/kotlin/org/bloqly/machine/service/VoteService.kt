package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
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
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val accountRepository: AccountRepository
) {

    private val log = LoggerFactory.getLogger(VoteService::class.simpleName)

    @Transactional
    fun findOrCreateVote(space: Space, validator: Account, passphrase: String): Vote? {

        val lastBlock = blockRepository.getLastBlock(space.id)

        return voteRepository.findBySpaceIdAndValidatorAndHeight(space.id, validator, lastBlock.height + 1)
            ?: createVote(validator, passphrase, lastBlock)
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
            validator = validator,
            blockHash = block.hash,
            height = block.height + 1,
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

    @Transactional
    fun isAcceptable(vote: Vote): Boolean {

        if (!spaceRepository.existsById(vote.spaceId)) {
            log.warn("Can't process vote, space doesn't exist ${vote.toVO()}")
            return false
        }

        if (voteRepository.existsBySpaceIdAndValidatorAndHeight(vote.spaceId, vote.validator, vote.height)) {
            log.warn("Vote with validator and height already exists ${vote.toVO()}")
            return false
        }

        if (voteRepository.existsByValidatorAndBlockHash(vote.validator, vote.blockHash)) {
            log.warn("Validator has already voted for this block ${vote.toVO()}")
        }

        return true
    }
}