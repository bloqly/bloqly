package org.bloqly.machine.test

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.component.GenesisService
import org.bloqly.machine.component.ObjectFilterService
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.lang.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.FinalizedTransactionRepository
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.PropertyService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.service.VoteService
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.block.BlockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.math.BigInteger.TEN

open class BaseTest {

    protected val maxSupply: BigInteger = BigInteger("1000000000").multiply(TEN.pow(8))

    @Autowired
    protected lateinit var blockchainService: BlockchainService

    @Autowired
    protected lateinit var genesisService: GenesisService

    @Autowired
    protected lateinit var accountService: AccountService

    @Autowired
    protected lateinit var spaceService: SpaceService

    @Autowired
    protected lateinit var objectFilterService: ObjectFilterService

    @Autowired
    protected lateinit var accountRepository: AccountRepository

    @Autowired
    protected lateinit var propertyService: PropertyService

    @Autowired
    protected lateinit var passphraseService: PassphraseService

    @Autowired
    protected lateinit var testService: TestService

    @Autowired
    protected lateinit var voteRepository: VoteRepository

    @Autowired
    protected lateinit var voteService: VoteService

    @Autowired
    protected lateinit var eventProcessorService: EventProcessorService

    @Autowired
    protected lateinit var eventReceiverService: EventReceiverService

    @Autowired
    protected lateinit var blockService: BlockService

    @Autowired
    protected lateinit var blockProcessor: BlockProcessor

    @Autowired
    protected lateinit var finalizedTransactionRepository: FinalizedTransactionRepository

    @Autowired
    protected lateinit var transactionRepository: TransactionRepository

    @Autowired
    protected lateinit var transactionOutputRepository: TransactionOutputRepository

    @Autowired
    protected lateinit var blockRepository: BlockRepository

    @Autowired
    protected lateinit var transactionService: TransactionService

    protected lateinit var propertyId: PropertyId

    @Before
    open fun setup() {
        testService.cleanup()
        TimeUtils.setTestTime(0)
        testService.createBlockchain()

        propertyId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, testService.getUser().accountId, "balance")
    }

    fun passphrase(accountId: String): String = passphraseService.getPassphrase(accountId)

    fun passphrase(account: Account): String = passphraseService.getPassphrase(account.accountId)

    fun validatorForRound(round: Long): Account {
        val space = spaceService.getById(DEFAULT_SPACE)
        return accountService.getProducerBySpace(space, round)!!
    }

    protected fun assertPropertyValueCandidate(value: String) {

        val lastValue = blockProcessor.getLastPropertyValue(
            DEFAULT_SPACE, DEFAULT_SELF, propertyId.target, propertyId.key
        )!!

        assertEquals(BInteger(value), ParameterUtils.readValue(lastValue))
    }

    protected fun assertPropertyValue(value: String) {
        val property: Property = propertyService.findById(propertyId)!!
        assertEquals(BInteger(value), ParameterUtils.readValue(property.value))
    }

    protected fun assertNoPropertyValue() {
        assertNull(propertyService.findById(propertyId))
    }

    fun getLIB(): Block {
        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)
        return blockService.getLIBForBlock(lastBlock)
    }

    fun onBlock(blockData: BlockData) = eventReceiverService.onBlocks(listOf(blockData))

    fun createNextBlock(spaceId: String, producer: Account, round: Long): BlockData =
        eventProcessorService.createNextBlock(spaceId, producer, round)

    fun createNextBlock(lastBlock: Block, producer: Account, round: Long): BlockData =
        eventProcessorService.createNextBlock(lastBlock, producer, round)
}