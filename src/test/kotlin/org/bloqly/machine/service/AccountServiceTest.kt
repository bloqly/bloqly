package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
class AccountServiceTest : BaseTest() {

    @Autowired
    private lateinit var accountService: AccountService

    private lateinit var account: Account

    private lateinit var space: Space

    private val testPassphrase = "test passphrase"

    @Before
    override fun setup() {
        super.setup()

        account = accountService.newAccount(testPassphrase)

        space = testService.getDefaultSpace()

        accountService.importAccount(
            CryptoUtils.decrypt(account.privateKeyEncoded, testPassphrase),
            testPassphrase
        )
    }

    @Test
    fun testGetActiveValidator() {
        val validator1 = accountService.getProducerBySpace(space, 0)
        val validator2 = accountService.getProducerBySpace(space, 1)
        val validator3 = accountService.getProducerBySpace(space, 2)
        val validator4 = accountService.getProducerBySpace(space, 3)
        val validator5 = accountService.getProducerBySpace(space, 4)

        assertNotNull(validator1)
        assertNotNull(validator2)
        assertNotNull(validator3)
        assertNotNull(validator4)
        assertNotNull(validator5)

        assertNotEquals(validator1, validator2)
        assertNotEquals(validator2, validator3)
        assertNotEquals(validator3, validator2)
        assertNotEquals(validator4, validator3)

        assertEquals(validator5, validator1)
    }

    @Test
    fun testNewAccount() {
        for (i in 0..4) {

            val passphrase = "passphrase $i"
            val account = accountService.newAccount(passphrase)

            println("id: ${account.id}")
            println("pub: ${account.publicKey!!.toLowerCase()}")
            println("priv: ${account.privateKey}")
            println("pass: $passphrase")
        }
    }

    @Test
    fun testImportAccountTwiceFails() {
        try {
            val passphrase = passphrase(account.accountId)
            accountService.importAccount(
                CryptoUtils.decrypt(account.privateKeyEncoded, passphrase),
                passphrase
            )
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun testGetAccountByPublicKey() {
        val passphrase = "passphrase"
        val account = accountService.newAccount(passphrase)

        val publicKey = account.publicKey!!

        accountRepository.save(account)

        val saved = accountService.getByPublicKey(publicKey)

        assertNotNull(saved)
        assertEquals(account, saved)
    }
}