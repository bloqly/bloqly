package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils.verifyTransaction
import org.bloqly.machine.test.TestUtils.FAKE_DATA
import org.bloqly.machine.vo.property.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class TransactionServiceTest : BaseTest() {

    private lateinit var transaction: Transaction

    private lateinit var user: Account

    private val passphrase = "password"

    @Before
    override fun setup() {
        super.setup()

        user = accountService.createAccount(passphrase)

        val lib = getLIB()

        transaction = createTransaction(lib.hash)
    }

    private fun createTransaction(referencedBlockHash: String): Transaction {
        return transactionService.createTransaction(

            space = DEFAULT_SPACE,

            originId = user.accountId,

            passphrase = passphrase,

            destinationId = user.accountId,

            self = user.accountId,

            key = null,

            value = Value.ofs("function init() {return [];}"),

            transactionType = TransactionType.CREATE,

            referencedBlockHash = referencedBlockHash,

            timestamp = Instant.now().toEpochMilli()
        )
    }

    @Test
    fun testGetPendingTransaction() {
        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val txs = blockProcessor.getPendingTransactions(lastBlock)
        assertEquals(1, txs.size)
        assertEquals(transaction.hash, txs.first().hash)

        testService.createTransaction()

        assertEquals(2, blockProcessor.getPendingTransactions(lastBlock).size)
    }

    @Test
    fun testIsActualTransactions() {

        val blocks = listOf(
            createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1).block,
            createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2).block,
            createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3).block
        )

        val tx = createTransaction(blocks[0].hash)

        assertFalse(blockService.isActualTransaction(tx, 0))
        assertFalse(blockService.isActualTransaction(tx, 1))
        assertTrue(blockService.isActualTransaction(tx, 2))
    }

    @Test
    fun testVerifyOK() {
        assertTrue(verifyTransaction(transaction))
    }

    @Test
    fun testVerifyDestinationWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(destination = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyOriginWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(origin = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyReferencedBlockIdWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(referencedBlockHash = FAKE_DATA)
            )
        )
    }

    @Test
    fun testVerifyTxTypeWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(transactionType = TransactionType.CALL)
            )
        )
    }

    @Test
    fun testVerifyAmountWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(value = listOf())
            )
        )
    }

    @Test
    fun testVerifyTimestampWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(timestamp = System.currentTimeMillis() + 1)
            )
        )
    }

    @Test
    fun testVerifyHashWrong() {
        assertFalse(verifyTransaction(transaction.copy(hash = FAKE_DATA)))
    }

    @Test
    fun testVerifySignatureWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(signature = transaction.signature.reversed())
            )
        )
    }

    @Test
    fun testVerifyPubKeyWrong() {
        verifyTransaction(
            transaction.copy(publicKey = transaction.publicKey.reversed())
        )
    }

    @Test
    fun testVerifySpaceWrong() {
        assertFalse(
            verifyTransaction(
                transaction.copy(spaceId = FAKE_DATA)
            )
        )
    }
}