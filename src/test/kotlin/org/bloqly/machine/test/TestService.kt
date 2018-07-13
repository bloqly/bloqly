package org.bloqly.machine.test

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.ResetService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.VoteVO
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
    private val resetService: ResetService,
    private val blockchainService: BlockchainService
) {

    private lateinit var accounts: List<Account>

    @PostConstruct
    @Suppress("unused")
    fun init() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        accounts = ObjectUtils.readValue(accountsString, Accounts::class.java).accounts
    }

    fun cleanup() {
        resetService.reset()
    }

    fun getRoot(): Account = accounts.first()

    fun getUser(): Account = accounts.last()

    fun getValidator(n: Int): Account = accounts[n + 1]

    fun createBlockchain() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        val accountsObject = ObjectUtils.readValue(accountsString, Accounts::class.java)

        accountsObject.accounts.forEach { account ->
            accountService.importAccount(account.privateKey!!)
        }

        blockchainService.createBlockchain(DEFAULT_SPACE, TEST_BLOCK_BASE_DIR)
    }

    fun createBlock(parentHash: String, height: Long, proposerId: String): Block {
        return blockRepository.save(
            Block(
                spaceId = DEFAULT_SPACE,
                height = height + 1,
                weight = 0,
                diff = 0,
                round = 0,
                timestamp = 0,
                parentHash = parentHash,
                proposerId = proposerId,
                validatorTxHash = byteArrayOf(),
                signature = byteArrayOf(),
                hash = CryptoUtils.hash(parentHash.toByteArray()).encode16(),
                libHash = CryptoUtils.hash(byteArrayOf()).encode16()
            )
        )
    }

    fun createTransaction(): Transaction {

        val lastBlock = blockRepository.getLastBlock(DEFAULT_SPACE)

        val root = accountRepository.findById(getRoot().id).orElseThrow()
        val user = accountRepository.findById(getUser().id).orElseThrow()

        return transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = root.id,
            destinationId = user.id,
            self = DEFAULT_SELF,
            value = writeLong("1"),
            transactionType = TransactionType.CALL,
            referencedBlockHash = lastBlock.hash,
            timestamp = Instant.now().toEpochMilli()
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

        val validatorsIds = validators.map { it.id }

        assertTrue(validatorsIds.contains(getValidator(0).id))
        assertTrue(validatorsIds.contains(getValidator(1).id))
        assertTrue(validatorsIds.contains(getValidator(2).id))
        assertTrue(validatorsIds.contains(getValidator(3).id))
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
        assertEquals(
            BigInteger.ONE,
            accountService.getAccountPower(DEFAULT_SPACE, getValidator(3).id)
        )
    }

    fun getVotes(): List<VoteVO> {
        return eventProcessorService.onGetVotes().map { it.toVO() }
    }

    fun getDefaultSpace(): Space {
        return spaceRepository.findById(DEFAULT_SPACE).orElseThrow()
    }

    @ValueObject
    private data class Accounts(val accounts: List<Account>)
}
