package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.CryptoUtils.verifyVote
import org.bloqly.machine.util.TestUtils.FAKE_DATA
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
class VoteServiceTest {

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var voteRepository: VoteRepository

    private lateinit var vote: Vote

    private lateinit var validator: Account

    private lateinit var validators: List<Account>

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        validators = accountService.getValidatorsForSpace(DEFAULT_SPACE)

        validator = validators.first()

        vote = voteService.createVote(DEFAULT_SPACE, validator)
    }

    @Test
    fun testNoDoubleVoteCreated() {
        val newVote = voteService.createVote(DEFAULT_SPACE, validator)
        assertEquals(newVote, vote)
    }

    @Test
    fun testVoteCreated() {

        val savedVote = voteRepository.findAll().first()

        val validator = validators.find { it.id == savedVote.id.validatorId }

        assertNotNull(validator)
    }

    @Test
    fun testVerifyVote() {

        assertTrue(verifyVote(vote))
    }

    @Test
    fun testVerifyVoteBlockWrongFails() {

        assertFalse(verifyVote(vote.copy(blockId = FAKE_DATA)))
    }

    @Test
    fun testVerifyVoteTimestampWrongFails() {

        assertFalse(verifyVote(vote.copy(timestamp = System.currentTimeMillis() + 1)))
    }

    @Test
    fun testVerifyVoteSignatureWrongFails() {

        assertFalse(verifyVote(vote.copy(signature = ByteArray(0))))
    }

    @Test
    fun testVerifyVoteValidatorWrongFails() {

        val newId = vote.id.copy(validatorId = FAKE_DATA)

        assertFalse(verifyVote(vote.copy(id = newId)))
    }

    @Test
    fun testVerifyVoteSpaceWrongFails() {

        val newId = vote.id.copy(space = FAKE_DATA)

        assertFalse(verifyVote(vote.copy(id = newId)))
    }

    @Test
    fun testVerifyVotHeightWrongFails() {

        val newId = vote.id.copy(height = vote.id.height + 1)

        assertFalse(verifyVote(vote.copy(id = newId)))
    }
}