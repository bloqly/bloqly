package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.CryptoUtils.verifyTransaction
import org.bloqly.machine.util.TestUtils.FAKE_DATA
import org.bloqly.machine.util.encode16
import org.junit.Assert.assertEquals
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

    @Autowired
    private lateinit var blockService: BlockService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var blockchainService: BlockchainService

    @Autowired
    private lateinit var blockProcessor: BlockProcessor

    private lateinit var transaction: Transaction

    private lateinit var root: Account

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        root = accountService.createAccount()

        transaction = createTransaction()
    }

    private fun createTransaction(referencedBlockHash: String = CryptoUtils.hash(arrayOf()).encode16()): Transaction {
        return transactionService.createTransaction(

            space = DEFAULT_SPACE,

            originId = root.accountId,

            destinationId = root.accountId,

            self = root.accountId,

            key = null,

            value = "true".toByteArray(),

            transactionType = TransactionType.CREATE,

            referencedBlockHash = referencedBlockHash,

            timestamp = Instant.now().toEpochMilli()
        )
    }

    @Test
    fun testGetPendingTransaction() {
        testService.createTransaction()

        val txs = transactionService.getPendingTransactionsBySpace(DEFAULT_SPACE, MAX_REFERENCED_BLOCK_DEPTH)

        assertEquals(1, txs.size)
    }

    @Test
    fun testGetPendingTransactions() {
        blockService.getLastBlockForSpace(DEFAULT_SPACE)

        blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(0), 1).block
        val block2 = blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(1), 2).block
        blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(2), 3).block
        val block4 = blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(3), 4).block
        val block5 = blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(0), 5).block
        blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(1), 6).block
        blockProcessor.createNextBlock(DEFAULT_SPACE, testService.getValidator(2), 7).block

        val lib = blockService.getLIBForSpace(DEFAULT_SPACE)
        assertEquals(block4.hash, lib.hash)

        val tx4 = createTransaction(block4.hash)
        assertTrue(blockchainService.isActualTransaction(tx4, 2))

        val tx2 = createTransaction(block2.hash)

        assertTrue(blockchainService.isActualTransaction(tx2, 2))
        assertTrue(blockchainService.isActualTransaction(tx2, 3))

        assertFalse(blockchainService.isActualTransaction(tx2, 1))

        // referencing non-lib block fails
        val tx5 = createTransaction(block5.hash)
        assertFalse(blockchainService.isActualTransaction(tx5, 1))
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
                transaction.copy(value = FAKE_DATA)
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