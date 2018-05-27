package org.bloqly.machine.component

import org.assertj.core.util.Sets
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.exception.SpaceAlreadyExistsException
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.junit.After
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
import java.math.BigInteger

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class EventProcessorServiceTest {

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var spaceRepository: SpaceRepository

    @Autowired
    private lateinit var propertyService: PropertyService

    @Autowired
    private lateinit var testService: TestService

    private lateinit var root: Account

    private lateinit var user: Account

    private lateinit var contractBody: String

    @Before
    fun setup() {

        assertFalse(spaceRepository.existsById(DEFAULT_SPACE))

        testService.createBlockchain()

        root = accountRepository.findById(testService.getRoot().id).get()
        user = accountRepository.findById(testService.getUser().id).get()

        contractBody = FileUtils.getResourceAsString("/scripts/test.js")
    }

    @After
    fun tearDown() {

        testService.cleanup()
    }

    @Test
    fun testPropertiesAreCreated() {
        assertEquals(2, propertyService.getQuorum(DEFAULT_SPACE))
    }

    @Test
    fun testSpaceCreated() {
        assertTrue(spaceRepository.existsById(DEFAULT_SPACE))
    }

    @Test
    fun testValidatorsInitialized() {
        val validators = accountService.getValidatorsForSpace(DEFAULT_SPACE)

        assertEquals(3, validators.size)

        val validatorsIds = validators.map { it.id }

        assertTrue(validatorsIds.contains(testService.getValidator(0).id))
        assertTrue(validatorsIds.contains(testService.getValidator(1).id))
        assertTrue(validatorsIds.contains(testService.getValidator(2).id))
    }

    @Test
    fun testValidatorsPowerValues() {
        assertEquals(BigInteger.ONE, accountService.getAccountPower(DEFAULT_SPACE, testService.getValidator(0).id))
        assertEquals(BigInteger.ONE, accountService.getAccountPower(DEFAULT_SPACE, testService.getValidator(1).id))
        assertEquals(BigInteger.ONE, accountService.getAccountPower(DEFAULT_SPACE, testService.getValidator(2).id))
    }

    @Test
    fun testInitTwiceFails() {
        try {
            eventProcessorService.createBlockchain(DEFAULT_SPACE, TEST_BLOCK_BASE_DIR)
            fail()
        } catch (e: Exception) {
            assertTrue(e.cause is SpaceAlreadyExistsException)
        }
    }

    @Test
    fun testInitTwiceWithDifferentSpaceOK() {
        eventProcessorService.createBlockchain("space1", TEST_BLOCK_BASE_DIR)
    }

    @Test
    fun testInitDefaultContract() {

        assertTrue(Sets.newHashSet(propertyRepository.findAll()).contains(
                Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.id, "balance"), writeLong("999997"))
        ))
    }

    @Test
    fun testMoveBalance() {

        val rootBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.id, "balance")
        val userBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, user.id, "balance")

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
}
