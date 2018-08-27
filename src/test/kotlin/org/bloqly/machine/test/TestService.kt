package org.bloqly.machine.test

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.component.ResetService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.bloqly.machine.vo.VoteVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import javax.annotation.PostConstruct
import javax.persistence.EntityManager

@Component
@Transactional
class TestService(
    private val propertyRepository: PropertyRepository,
    private val blockService: BlockService,
    private val spaceRepository: SpaceRepository,
    private val eventProcessorService: EventProcessorService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val accountService: AccountService,
    private val resetService: ResetService,
    private val blockchainService: BlockchainService,
    private val entityManager: EntityManager,
    private val passphraseService: PassphraseService
) {

    private lateinit var accounts: List<Account>

    @PostConstruct
    @Suppress("unused")
    fun init() {
        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        accounts = ObjectUtils.readValue(accountsString, Accounts::class.java).accounts
    }

    fun cleanup(deleteAccounts: Boolean = true) {
        resetService.reset(deleteAccounts = deleteAccounts)
    }

    fun getRoot(): Account = accounts.first()

    fun getUser(): Account = accounts.last()

    fun getValidator(n: Int): Account = accounts[n + 1]

    fun importAccounts() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        val accountsObject = ObjectUtils.readValue(accountsString, Accounts::class.java)

        accountsObject.accounts.forEach { account ->
            val passphrase = passphraseService.getPassphrase(account.accountId)
            accountService.importAccount(account.privateKeyEncoded, passphrase)
        }
    }

    fun createBlockchain() {

        importAccounts()

        blockchainService.createBlockchain(
            DEFAULT_SPACE,
            TEST_BLOCK_BASE_DIR,
            passphraseService.getPassphrase(getRoot().accountId)
        )
    }

    fun createTransaction(): Transaction {

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val lib = blockService.getLIBForBlock(lastBlock)

        val libHash = lib.hash

        val root = accountRepository.findByAccountId(getRoot().accountId)!!
        val user = accountRepository.findByAccountId(getUser().accountId)!!

        return transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = root.accountId,
            passphrase = passphraseService.getPassphrase(root.accountId),
            destinationId = user.accountId,
            self = DEFAULT_SELF,
            value = writeLong("1"),
            transactionType = TransactionType.CALL,
            referencedBlockHash = libHash
        )
    }

    fun createInvalidTransaction(): Transaction {

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val lib = blockService.getLIBForBlock(lastBlock)

        val root = accountRepository.findByAccountId(getRoot().accountId)!!
        val user = accountRepository.findByAccountId(getUser().accountId)!!

        return transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = user.accountId,
            passphrase = passphraseService.getPassphrase(user.accountId),
            destinationId = root.accountId,
            self = DEFAULT_SELF,
            // got no money
            value = writeLong("100"),
            transactionType = TransactionType.CALL,
            referencedBlockHash = lib.hash
        )
    }

    fun testPropertiesAreCreated() {
        assertEquals(3, propertyRepository.getQuorumBySpace(getDefaultSpace()))
    }

    fun testSpaceCreated() {
        assertTrue(spaceRepository.existsById(DEFAULT_SPACE))
    }

    fun testValidatorsInitialized() {
        val validators = accountService.getValidatorsForSpace(getDefaultSpace())

        assertEquals(4, validators.size)

        val validatorsIds = validators.map { it.accountId }

        assertTrue(validatorsIds.contains(getValidator(0).accountId))
        assertTrue(validatorsIds.contains(getValidator(1).accountId))
        assertTrue(validatorsIds.contains(getValidator(2).accountId))
        assertTrue(validatorsIds.contains(getValidator(3).accountId))
    }

    fun testValidatorsPowerValues() {
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(0).accountId)
        )
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(1).accountId)
        )
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(2).accountId)
        )
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(3).accountId)
        )
    }

    fun getVotes(): List<VoteVO> {
        return eventProcessorService.onGetVotes().map { it.toVO() }
    }

    fun getDefaultSpace(): Space {
        return spaceRepository.findById(DEFAULT_SPACE).orElseThrow()
    }

    fun cleanupBlockTransactions() {
        entityManager.createNativeQuery("delete from block_transactions").executeUpdate()
    }

    @ValueObject
    private data class Accounts(val accounts: List<Account>)
}
