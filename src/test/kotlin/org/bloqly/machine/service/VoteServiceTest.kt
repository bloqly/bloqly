package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Vote
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils.verifyVote
import org.bloqly.machine.util.TestUtils.FAKE_DATA
import org.bloqly.machine.util.decode16
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class VoteServiceTest : BaseTest() {

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var accountService: AccountService

    private lateinit var vote: Vote

    private lateinit var publicKey: ByteArray

    private lateinit var validators: List<Account>

    private lateinit var space: Space

    private lateinit var validator: Account

    @Before
    override fun setup() {
        super.setup()

        space = testService.getDefaultSpace()

        validators = accountService.getValidatorsForSpace(space)

        validator = validators.first()

        publicKey = validator.publicKey.decode16()

        vote = voteService.findOrCreateVote(space, validator, passphrase(validator.accountId))!!
    }

    @Test
    fun testNoDoubleVoteCreated() {
        val v1 = voteService.findOrCreateVote(space, validator, passphrase(validator.accountId))
        val v2 = voteService.findOrCreateVote(space, validator, passphrase(validator.accountId))

        assertEquals(v1, v2)
    }

    @Test
    fun testVoteCreated() {

        val savedVote = voteRepository.findAll().first()

        val validator = validators.find { it.accountId == savedVote.validator.accountId }

        assertNotNull(validator)
    }

    @Test
    fun testVerifyVote() {

        val converted = vote.toVO().toModel(validator)

        assertEquals(vote.validator, converted.validator)
        assertEquals(vote.blockHash, converted.blockHash)
        assertEquals(vote.height, converted.height)
        assertEquals(vote.timestamp, converted.timestamp)
        assertArrayEquals(vote.signature, converted.signature)

        assertTrue(verifyVote(vote, publicKey))
    }

    @Test
    fun testVerifyVoteBlockWrongFails() {

        assertFalse(verifyVote(vote.copy(blockHash = FAKE_DATA), publicKey))
    }

    @Test
    fun testVerifyVoteTimestampWrongFails() {

        assertFalse(verifyVote(vote.copy(timestamp = System.currentTimeMillis() + 1), publicKey))
    }

    @Test
    fun testVerifyVoteSignatureWrongFails() {

        assertFalse(verifyVote(vote.copy(signature = ByteArray(0)), publicKey))
    }

    @Test
    fun testVerifyVoteValidatorWrongFails() {

        val wrongValidator = validator.copy(accountId = FAKE_DATA)

        assertFalse(verifyVote(vote.copy(validator = wrongValidator), publicKey))
    }

    @Test
    fun testVerifyVotHeightWrongFails() {

        assertFalse(verifyVote(vote.copy(height = vote.height + 1), publicKey))
    }
}