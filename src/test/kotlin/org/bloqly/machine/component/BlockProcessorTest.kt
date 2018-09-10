package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.lang.BLong
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.TimeUtils.setTestTime
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.block.BlockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockProcessorTest : BaseTest() {

    private val blocks = mutableListOf<BlockData>()

    private lateinit var firstBlock: Block

    private val txs = mutableListOf<Transaction>()

    private lateinit var space: Space

    @Before
    override fun setup() {
        super.setup()

        firstBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)
        space = spaceService.getById(DEFAULT_SPACE)
    }

    @Test
    fun testGetPendingTransactionsReturnsNotIncludedInBlock() {

        testService.createTransaction()
        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        testService.createTransaction()
        createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)

        val tx3 = testService.createTransaction()

        val txs = blockProcessor.getPendingTransactions()

        assertEquals(1, txs.size)
        assertEquals(tx3, txs.first())
    }

    @Test
    fun testDoesntMoveLIBTwice() {

        eventProcessorService.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)
        eventProcessorService.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)
        eventProcessorService.createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)

        testService.createTransaction()
        val blockData = eventProcessorService.createNextBlock(DEFAULT_SPACE, validatorForRound(4), 4)

        assertFalse(blockService.isAcceptable(blockData.toModel()))

        try {
            blockProcessor.processReceivedBlock(blockData)
            fail()
        } catch (e: Exception) {

        }

        eventProcessorService.onProposal(blockData)
    }

    @Test
    fun testLIBIsNotMovingBack() {
        val blockData1 = createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        eventProcessorService.onGetVotes()
        val blockData2 = createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2) // hyper-finalizer

        eventProcessorService.onGetVotes()
        createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3) // same validator

        val blockData4 = createNextBlock(DEFAULT_SPACE, validatorForRound(7), 7) // same validator

        assertEquals(blockData1.block.height, blockData4.block.libHeight)

        val lastBlock = blockService.getByHash(blockData4.block.hash)

        val calculatedLIB = blockService.calculateLIBForBlock(lastBlock.hash)

        assertEquals(blockData2.block.libHeight, calculatedLIB.libHeight)
    }

    @Test
    fun testLIBNoVotes() {

        val firstBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)

        val blockData3 = createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)

        assertEquals(firstBlock.height, blockData3.block.libHeight)

        val lastBlock = blockService.getByHash(blockData3.block.hash)

        val calculatedLIB = blockService.calculateLIBForBlock(lastBlock.hash)

        assertEquals(firstBlock.height, calculatedLIB.libHeight)
    }

    @Test
    fun testGetPendingTransactionsReturnsTxAfterLIB() {
        val tx = testService.createTransaction()

        val txs = blockProcessor.getPendingTransactions()

        assertEquals(1, txs.size)
        assertEquals(tx, txs.first())
    }

    @Test
    fun testGetPendingTransactionsReturnsTxOnlyCurrentBranch() {

        val tx = testService.createTransaction()

        val blockBranch1 = createNextBlock(firstBlock, validatorForRound(2), 2)
        assertNotNull(blockBranch1.transactions.find { it.hash == tx.hash })

        val blockBranch2 = createNextBlock(firstBlock, validatorForRound(2), 2)
        assertNotNull(blockBranch2.transactions.find { it.hash == tx.hash })

        val txs1 = blockProcessor.getPendingTransactions(blockBranch1.toModel())
        assertEquals(1, txs1.size)

        val txs2 = blockProcessor.getPendingTransactions(blockBranch2.toModel())
        assertEquals(1, txs2.size)
    }

    @Test
    fun testGetBlockRange() {
        populateBlocks(cleanup = false)

        val blocksRange = blockProcessor.getBlocksFromLIB(
            blocks.last().block.toModel()
        )

        val blockHashes = blocks.drop(3).map { it.block.hash }
        val rangeBlockHashes = blocksRange.map { it.hash }

        assertEquals(blockHashes, rangeBlockHashes)
    }

    @Test
    fun testVerifyBlock() {
        val blockData = createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)
        val producer = accountRepository.findByAccountId(blockData.block.producerId)!!

        val block = blockRepository.findByHash(blockData.block.hash)!!

        assertTrue(CryptoUtils.verifyBlock(block, producer.publicKey.decode16()))
    }

    @Test
    fun testBlockProcessed() {
        populateBlocks()
        setTestTime(Application.ROUND + 1L)

        assertNull(propertyService.findById(propertyId))

        onBlock(blocks[0])

        assertNotNull(blockRepository.findByHash(blocks[0].block.hash))

        assertNull(propertyService.findById(propertyId))
    }

    @Test
    fun testProcessNewBlockWithOldVotes() {
        populateBlocks()

        val votes = blocks[0].votes.map { it.toModel() }

        voteRepository.saveAll(votes)

        TimeUtils.setTestRound(blocks[0].block.round)
        eventReceiverService.onBlocks(listOf(blocks[0]))
    }

    @Test
    fun testBlockWithManyTransactions() {
        populateBlocks()

        val txCount = 100

        repeat(txCount) {
            testService.createTransaction()
        }

        val blockData = createNextBlock(DEFAULT_SPACE, validatorForRound(9), 9)

        assertEquals(txCount, blockData.transactions.size)
    }

    @Test
    fun testRejectsBlockWithTheSameHash() {
        populateBlocks()

        val blockData = blocks[0]

        assertTrue(blockService.isAcceptable(blockData.toModel()))

        TimeUtils.setTestRound(blockData.block.round)

        eventReceiverService.onBlocks(listOf(blockData))

        assertFalse(blockService.isAcceptable(blockData.toModel()))
    }

    @Test
    fun testInvalidTransactionNotIncluded() {

        testService.createTransaction()
        testService.createInvalidTransaction()

        val block = createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        assertEquals(1, block.transactions.size)
    }

    @Test
    fun testPropertyAppliedWhenLIBChanged() {
        populateBlocks()

        eventReceiverService.onBlocks(listOf(blocks[0]))
        assertTrue(blockService.existsByHash(blocks[0].block.hash))

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[1]))
        assertTrue(blockService.existsByHash(blocks[1].block.hash))

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[2]))
        assertTrue(blockService.existsByHash(blocks[2].block.hash))

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[3]))
        assertTrue(blockService.existsByHash(blocks[3].block.hash))

        assertPropertyValueCandidate("4")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[4]))
        assertTrue(blockService.existsByHash(blocks[4].block.hash))

        assertPropertyValueCandidate("5")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[5]))
        assertTrue(blockService.existsByHash(blocks[5].block.hash))

        assertPropertyValueCandidate("6")
        assertNoPropertyValue()

        eventReceiverService.onBlocks(listOf(blocks[6]))
        assertTrue(blockService.existsByHash(blocks[6].block.hash))

        assertPropertyValueCandidate("7")
        assertPropertyValue("1")
    }

    private fun createAndAssertBlock(n: Long, libHeight: Long = 0) {

        assertTrue(n > 0)

        val lib = getLIB()

        val tx = testService.createTransaction()
        val blockData = createNextBlock(DEFAULT_SPACE, validatorForRound(n), n)
        val block = blockData.block

        assertEquals(1, blockData.transactionOutputs.size)
        val properties = ObjectUtils.readProperties(blockData.transactionOutputs[0].output)

        assertEquals(2, properties.size)

        val value1 = getTxOutputValue(blockData, 0)
        val value2 = getTxOutputValue(blockData, 1)

        assertEquals(maxSupply.toLong() - n - 4, value1)
        assertEquals(n, value2)

        assertEquals(tx.hash, blockData.transactions.first().hash)
        assertEquals(block.producerId, accountService.getProducerBySpace(space, n)!!.accountId)
        assertEquals(libHeight, block.libHeight)

        assertEquals(lib.hash, tx.referencedBlockHash)

        assertEquals(libHeight, getLIB().height)

        blocks.add(blockData)
        txs.add(tx)
    }

    private fun getTxOutputValue(blockData: BlockData, n: Int): Long {
        val properties = ObjectUtils.readProperties(blockData.transactionOutputs[0].output)

        val bInt = ParameterUtils.readValue(properties[n].value) as BLong
        return bInt.value.toLong()
    }

    private fun populateBlocks(cleanup: Boolean = true) {

        // BLOCK 1
        createAndAssertBlock(1)

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        // BLOCK 2
        createAndAssertBlock(2)

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        // BLOCK 3
        createAndAssertBlock(3)

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        // BLOCK 4
        createAndAssertBlock(4)

        assertPropertyValueCandidate("4")
        assertNoPropertyValue()

        // BLOCK 5
        createAndAssertBlock(5)

        assertPropertyValueCandidate("5")
        assertNoPropertyValue()

        // BLOCK 6
        createAndAssertBlock(6)

        assertPropertyValueCandidate("6")
        assertNoPropertyValue()

        // BLOCK 7
        createAndAssertBlock(7, 1)

        assertPropertyValueCandidate("7")
        assertPropertyValue("1")

        // BLOCK 8
        createAndAssertBlock(8, 2)

        assertPropertyValueCandidate("8")
        assertPropertyValue("2")

        // BLOCK 9
        createAndAssertBlock(9, 3)

        assertPropertyValueCandidate("9")
        assertPropertyValue("3")

        if (cleanup) {
            val genesis = genesisService.exportFirst(DEFAULT_SPACE)
            testService.cleanup()
            testService.importAccounts()
            genesisService.importFirst(genesis)
        }
    }
}