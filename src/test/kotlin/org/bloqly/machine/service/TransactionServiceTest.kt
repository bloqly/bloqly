package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.util.CryptoUtils.isTransactionValid
import org.bloqly.machine.util.TestUtils.FAKE_DATA
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant

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

            originId = root.id,

            destinationId = root.id,

            self = root.id,

            key = null,

            value = "true".toByteArray(),

            transactionType = TransactionType.CREATE,

            referencedBlockId = "",

            timestamp = Instant.now().toEpochMilli()

        )
    }

    @Test
    fun testVerifyOK() {

        assertTrue(isTransactionValid(transaction))
    }

    @Test
    fun testVerifyDestinationWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(destination = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyOriginWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(origin = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyReferencedBlockIdWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(referencedBlockId = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyTxTypeWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(transactionType = TransactionType.CALL)
            )
        )
    }

    @Test
    fun testVerifyAmountWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(value = FAKE_DATA.toByteArray())
            )
        )
    }

    @Test
    fun testVerifyTimestampWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(timestamp = System.currentTimeMillis() + 1)
            )
        )
    }

    @Test
    fun testVerifyIdWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(id = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifySignatureWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(signature = transaction.signature.reversed().toByteArray())
            )
        )
    }

    @Test
    fun testVerifyPubKeyWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(publicKey = transaction.publicKey.reversed())
            )
        )
    }

    @Test
    fun testVerifySpaceWrong() {

        assertFalse(
            isTransactionValid(
                transaction.copy(spaceId = FAKE_DATA)
            )
        )
    }
}