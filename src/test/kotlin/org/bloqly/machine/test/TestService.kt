package org.bloqly.machine.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.ResetService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant
import javax.annotation.PostConstruct
import javax.transaction.Transactional

@Component
@Transactional
class TestService(
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val eventProcessorService: EventProcessorService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper,
    private val resetService: ResetService
) {

    private lateinit var accounts: List<Account>

    @PostConstruct
    @Suppress("unused")
    fun init() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        accounts = objectMapper.readValue(accountsString, Accounts::class.java).accounts
    }

    fun cleanup() {
        resetService.reset()
    }

    fun getRoot(): Account = accounts.first()

    fun getUser(): Account = accounts.last()

    fun getValidator(n: Int): Account = accounts[n + 1]

    fun createBlockchain() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        val accountsObject = objectMapper.readValue(accountsString, Accounts::class.java)

        accountsObject.accounts.forEach { account ->
            accountService.importAccount(account.privateKey!!)
        }

        eventProcessorService.createBlockchain(Application.DEFAULT_SPACE, TEST_BLOCK_BASE_DIR)
    }

    fun newTransaction(): Transaction {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(DEFAULT_SPACE)

        val root = accountRepository.findById(getRoot().id).orElseThrow()
        val user = accountRepository.findById(getUser().id).orElseThrow()

        return transactionService.newTransaction(
            space = DEFAULT_SPACE,
            originId = root.id,
            destinationId = user.id,
            value = writeLong("1"),
            transactionType = TransactionType.CALL,
            referencedBlockId = lastBlock.id,
            timestamp = Instant.now().toEpochMilli()
        )
    }

    fun testPropertiesAreCreated() {
        assertEquals(2, propertyRepository.getQuorum(DEFAULT_SPACE))
    }

    fun testSpaceCreated() {
        assertTrue(spaceRepository.existsById(DEFAULT_SPACE))
    }

    fun testValidatorsInitialized() {
        val validators = accountService.getValidatorsForSpace(DEFAULT_SPACE)

        assertEquals(3, validators.size)

        val validatorsIds = validators.map { it.id }

        assertTrue(validatorsIds.contains(getValidator(0).id))
        assertTrue(validatorsIds.contains(getValidator(1).id))
        assertTrue(validatorsIds.contains(getValidator(2).id))
    }

    fun testValidatorsPowerValues() {
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(0).id)
        )
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(1).id)
        )
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(2).id)
        )
    }

    @ValueObject
    private data class Accounts(val accounts: List<Account>)
}
