package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.util.CryptoUtils.verifyTransaction
import org.bloqly.machine.util.TestUtils.FAKE_DATA
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.ZoneOffset
import java.time.ZonedDateTime

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class TransactionServiceTest {

    @Autowired
    private lateinit var transactionService: TransactionService

    @Autowired
    private lateinit var accountService: AccountService

    private lateinit var transaction: Transaction

    @Before
    fun init() {

        val root = accountService.createAccount()

        transaction = transactionService.newTransaction(

                space = DEFAULT_SPACE,

                origin = root,

                destinationId = root.id,

                self = root.id,

                key = null,

                value = "true".toByteArray(),

                transactionType = TransactionType.CREATE,

                referencedBlockId = "",

                timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()

        )
    }

    @Test
    fun testVerifyOK() {

        assertTrue(verifyTransaction(transaction))
    }

    @Test
    fun testVerifyDestinationWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(destination = FAKE_DATA)
        ))
    }

    @Test
    fun testVerifyOriginWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(origin = FAKE_DATA)
        ))
    }

    @Test
    fun testVerifyReferencedBlockIdWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(referencedBlockId = FAKE_DATA)
        ))
    }


    @Test
    fun testVerifyTxTypeWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(transactionType = TransactionType.CALL)
        ))
    }

    @Test
    fun testVerifyAmountWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(value = FAKE_DATA.toByteArray())
        ))
    }

    @Test
    fun testVerifyTimestampWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(timestamp = System.currentTimeMillis() + 1)
        ))
    }

    @Test
    fun testVerifyIdWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(id = FAKE_DATA)
        ))
    }

    @Test
    fun testVerifySignatureWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(signature = transaction.signature.reversed().toByteArray())
        ))
    }

    @Test
    fun testVerifyPubKeyWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(publicKey = transaction.publicKey.reversed())
        ))
    }

    @Test
    fun testVerifySpaceWrong() {

        assertFalse(verifyTransaction(
                transaction.copy(space = FAKE_DATA)
        ))
    }

}