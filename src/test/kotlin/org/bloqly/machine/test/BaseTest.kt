package org.bloqly.machine.test

import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.model.Account
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.TimeUtils
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

open class BaseTest {

    @Autowired
    protected lateinit var passphraseService: PassphraseService

    @Autowired
    protected lateinit var testService: TestService

    @Autowired
    protected lateinit var voteRepository: VoteRepository

    @Autowired
    protected lateinit var eventProcessorService: EventProcessorService

    @Autowired
    protected lateinit var blockService: BlockService

    @Autowired
    protected lateinit var blockProcessor: BlockProcessor

    @Autowired
    protected lateinit var transactionRepository: TransactionRepository

    @Autowired
    protected lateinit var transactionService: TransactionService

    @Before
    open fun setup() {
        TimeUtils.setTestTime(0)
        testService.cleanup()
        testService.createBlockchain()
    }

    fun passphrase(accountId: String): String = passphraseService.getPassphrase(accountId)

    fun passphrase(account: Account): String = passphraseService.getPassphrase(account.accountId)

    fun validator(n: Int) = testService.getValidator(n)

    fun passphrase(n: Int) = passphrase(validator(n))
}