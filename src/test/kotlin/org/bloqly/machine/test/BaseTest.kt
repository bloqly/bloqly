package org.bloqly.machine.test

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.component.ObjectFilterService
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockData
import org.junit.Assert
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

open class BaseTest {

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
    protected lateinit var eventProcessorService: EventProcessorService

    @Autowired
    protected lateinit var eventReceiverService: EventReceiverService

    @Autowired
    protected lateinit var blockService: BlockService

    @Autowired
    protected lateinit var blockProcessor: BlockProcessor

    @Autowired
    protected lateinit var transactionRepository: TransactionRepository

    @Autowired
    protected lateinit var transactionService: TransactionService

    protected lateinit var propertyId: PropertyId

    @Before
    open fun setup() {
        TimeUtils.setTestTime(0)
        testService.cleanup()
        testService.createBlockchain()

        propertyId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, testService.getUser().accountId, "balance")
    }

    fun passphrase(accountId: String): String = passphraseService.getPassphrase(accountId)

    fun passphrase(account: Account): String = passphraseService.getPassphrase(account.accountId)

    fun validator(n: Int) = testService.getValidator(n)

    fun passphrase(n: Int) = passphrase(validator(n))

    protected fun assertPropertyValueCandidate(value: String) {

        val lastValue = blockProcessor.getLastPropertyValue(
            DEFAULT_SPACE, DEFAULT_SELF, propertyId.target, propertyId.key
        )!!

        Assert.assertEquals(BInteger(value), ParameterUtils.readValue(lastValue))
    }

    protected fun assertPropertyValue(value: String) {
        val property: Property = propertyService.findById(propertyId)!!
        Assert.assertEquals(BInteger(value), ParameterUtils.readValue(property.value))
    }

    protected fun assertNoPropertyValue() {
        Assert.assertNull(propertyService.findById(propertyId))
    }

    fun getLIB(): Block {
        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)
        return blockService.getByHash(lastBlock.libHash)
    }

    fun onBlock(blockData: BlockData) = eventReceiverService.onBlocks(listOf(blockData))
}