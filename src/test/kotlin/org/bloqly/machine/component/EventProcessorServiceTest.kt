package org.bloqly.machine.component

import org.assertj.core.util.Sets
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.property.Value
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
class EventProcessorServiceTest : BaseTest() {

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var contractService: ContractService

    @Autowired
    private lateinit var transactionProcessor: TransactionProcessor

    @Autowired
    private lateinit var spaceRepository: SpaceRepository

    private lateinit var root: Account

    private lateinit var user: Account

    @Before
    override fun setup() {
        super.setup()

        root = accountRepository.findByAccountId(testService.getRoot().accountId)!!
        user = accountRepository.findByAccountId(testService.getUser().accountId)!!
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
            blockchainService.createBlockchain(
                DEFAULT_SPACE, TEST_BLOCK_BASE_DIR, passphrase(root.accountId)
            )
            fail()
        } catch (e: Exception) {
            // pass
        }
    }

    @Test
    fun testInitTwiceWithDifferentSpaceOK() {
        TimeUtils.testTick()
        blockchainService.createBlockchain(
            "space1", TEST_BLOCK_BASE_DIR, passphrase(root.accountId)
        )
    }

    @Test
    fun testInitDefaultContract() {

        val balance = Value.of(maxSupply.minus(BigInteger("4")))

        assertTrue(
            Sets.newHashSet(propertyRepository.findAll()).contains(
                Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.accountId, "balance"), balance)
            )
        )
    }

    @Test
    fun testMoveBalance() {

        val rootBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, root.accountId, "balance")
        val userBalanceId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, user.accountId, "balance")

        val rootBalanceBefore = propertyRepository.findById(rootBalanceId).orElseThrow()
        val userBalanceBefore = propertyRepository.findById(userBalanceId)

        assertEquals(Value.of(maxSupply.minus(BigInteger("4"))), rootBalanceBefore.value)
        assertFalse(userBalanceBefore.isPresent)

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        TimeUtils.testTick()

        val tx = transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = root.accountId,
            passphrase = passphrase(root.accountId),
            destinationId = user.accountId,
            self = DEFAULT_SELF,
            value = Value.ofs(1),
            transactionType = TransactionType.CALL,
            referencedBlockHash = lastBlock.hash
        )

        val propertyContext = PropertyContext(propertyService, contractService)

        transactionProcessor.processTransaction(tx, propertyContext)

        propertyContext.commit()

        val rootBalanceAfter = propertyRepository.findById(rootBalanceId).orElseThrow()
        val userBalanceAfter = propertyRepository.findById(userBalanceId).orElseThrow()

        assertEquals(Value.of(maxSupply.minus(BigInteger("5"))), rootBalanceAfter.value)
        assertEquals(Value.of(BigInteger("1")), userBalanceAfter.value)
    }

    @Test
    fun testReturnSameProposalsInSingleRound() {
        TimeUtils.setTestTime(Application.ROUND + 1L)

        val votes = testService.getVotes()

        assertEquals(4, votes.size)

        val proposals1 = eventProcessorService.onProduceBlock()

        val proposals2 = eventProcessorService.onProduceBlock()

        assertEquals(1, proposals1.size)
        assertEquals(1, proposals2.size)

        assertEquals(proposals1.first().block, proposals2.first().block)

        // TODO votes and transactions
    }
}
