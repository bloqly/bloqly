package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.Application.Companion.MAX_EMPTY_ROUND_COUNTER
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.EmptyRound
import org.bloqly.machine.model.RoundId
import org.bloqly.machine.repository.EmptyRoundRepository
import org.bloqly.machine.test.TestService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class AccountServiceTest {

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var emptyRoundRepository: EmptyRoundRepository

    @Autowired
    private lateinit var testService: TestService

    private lateinit var account: Account

    @Before
    fun setup() {
        testService.cleanup()
        testService.createBlockchain()

        account = accountService.newAccount()

        accountService.importAccount(account.privateKey!!)
    }

    @Test
    fun testGetActiveValidator() {
        assertNotNull(accountService.getActiveValidator(DEFAULT_SPACE, 1))
        assertNotNull(accountService.getActiveValidator(DEFAULT_SPACE, 2))
        assertNotNull(accountService.getActiveValidator(DEFAULT_SPACE, 3))
        assertNotNull(accountService.getActiveValidator(DEFAULT_SPACE, 4))
    }

    @Test
    fun testNewAccount() {

        for (i in 0..4) {

            val account = accountService.newAccount()

            println("id: ${account.id}")
            println("pub: ${account.publicKey!!.toLowerCase()}")
            println("priv: ${account.privateKey}")
        }
    }

    @Test
    fun testImportAccountTwiceFails() {
        try {
            accountService.importAccount(account.privateKey!!)
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun testNextValidatorSelectedAfterMissedRound() {

        val validator2 = accountService.getActiveValidator(DEFAULT_SPACE, 2)

        emptyRoundRepository.save(
            EmptyRound(
                id = RoundId(DEFAULT_SPACE, 1),
                counter = MAX_EMPTY_ROUND_COUNTER,
                lastMissTime = 0
            )
        )

        accountService.getValidatorsForSpace(DEFAULT_SPACE)

        val validator1Temp = accountService.getActiveValidator(DEFAULT_SPACE, 1)

        assertEquals(validator1Temp, validator2)

        val validator2Normal = accountService.getActiveValidator(DEFAULT_SPACE, 2)

        assertEquals(validator2Normal, validator2)
    }
}