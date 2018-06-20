package org.bloqly.machine.component

import org.assertj.core.util.Sets
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.RoundRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class EventProcessorServiceTest {

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var spaceRepository: SpaceRepository

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var roundRepository: RoundRepository

    private lateinit var root: Account

    private lateinit var user: Account

    @Before
    fun setup() {
        testService.cleanup()

        assertFalse(spaceRepository.existsById(DEFAULT_SPACE))

        testService.createBlockchain()

        root = accountRepository.findById(testService.getRoot().id).get()
        user = accountRepository.findById(testService.getUser().id).get()
    }

    @Test
    fun testPropertiesAreCreated() {
        testService.testPropertiesAreCreated()
    }

    @Test
    fun testSpaceCreated() {
        testService.testSpaceCreated()
    }

    @Test
    fun testValidatorsInitialized() {
        testService.testValidatorsInitialized()
    }

    @Test
    fun testValidatorsPowerValues() {
        testService.testValidatorsPowerValues()
    }

    @Test
    fun testInitTwiceFails() {
        try {
            eventProcessorService.createBlockchain(DEFAULT_SPACE, TEST_BLOCK_BASE_DIR)
            fail()
        } catch (e: Exception) {
            // pass
        }
    }

    @Test
    fun testInitTwiceWithDifferentSpaceOK() {
        eventProcessorService.createBlockchain("space1", TEST_BLOCK_BASE_DIR)
    }

    @Test
    fun testInitDefaultContract() {

        assertTrue(
            Sets.newHashSet(propertyRepository.findAll()).contains(
                Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.id, "balance"), writeLong("999997"))
            )
        )
    }

    @Test
    fun testMoveBalance() {

        val rootBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.id, "balance")
        val userBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, user.id, "balance")

        val rootBalanceBefore = propertyRepository.findById(rootBalanceId).orElseThrow()
        val userBalanceBefore = propertyRepository.findById(userBalanceId)

        assertArrayEquals(writeLong("999997"), rootBalanceBefore.value)
        assertFalse(userBalanceBefore.isPresent)

        val transaction = TestUtils.createTransaction(
            origin = root.id,
            destination = user.id,
            value = writeLong("1")
        )

        eventProcessorService.processTransaction(transaction)

        val rootBalanceAfter = propertyRepository.findById(rootBalanceId).orElseThrow()
        val userBalanceAfter = propertyRepository.findById(userBalanceId).orElseThrow()

        assertArrayEquals(writeLong("999996"), rootBalanceAfter.value)
        assertArrayEquals(writeLong("1"), userBalanceAfter.value)
    }

    @Test
    fun testReturnSameProposals() {
        val votes = testService.getVotes()

        assertEquals(3, votes.size)

        val proposals1 = eventProcessorService.onGetProposals()

        val proposals2 = eventProcessorService.onGetProposals()

        assertEquals(1, proposals1.size)

        assertEquals(proposals1, proposals2)
    }
}
