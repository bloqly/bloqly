package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.TimeUtils.setTestTime
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.BlockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockProcessorTest : BaseTest() {

    @Autowired
    private lateinit var transactionOutputRepository: TransactionOutputRepository

    private val blocks = mutableListOf<BlockData>()

    private lateinit var firstBlock: Block

    private val txs = arrayOfNulls<Transaction>(8)

    @Before
    override fun setup() {
        super.setup()

        firstBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)
    }

    @Test
    fun testGetPendingTransactionsReturnsNotIncludedInBlock() {

        testService.createTransaction()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        testService.createTransaction()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)

        val tx3 = testService.createTransaction()

        val txs = blockProcessor.getPendingTransactions()

        assertEquals(1, txs.size)
        assertEquals(tx3, txs.first())
    }

    @Test
    fun testDoesntMoveLIBTwice() {

        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)

        testService.createTransaction()
        val blockData = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(4), 4)

        assertFalse(blockService.isAcceptable(blockData.block.toModel()))

        try {
            blockProcessor.processReceivedBlock(blockData)
            fail()
        } catch (e: Exception) {

        }

        eventProcessorService.onProposal(blockData)
    }

    @Test
    fun testLIBIsNotMovingBack() {
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1) // normally finalized

        eventProcessorService.onGetVotes()
        val blockData2 = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2) // hyper-finalized

        eventProcessorService.onGetVotes()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3) // same validator

        val blockData4 = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(7), 7) // same validator

        val lib = blockService.getByHash(blockData4.block.libHash)

        assertEquals(blockData2.block.hash, lib.hash)

        val lastBlock = blockService.getByHash(blockData4.block.hash)

        val calculatedLIB = blockService.calculateLIBForBlock(lastBlock.hash)

        assertEquals(blockData2.block.libHash, calculatedLIB.libHash)
    }

    @Test
    fun testLIBNoVotes() {

        val firstBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)

        val blockData3 = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)

        assertEquals(firstBlock.hash, blockData3.block.libHash)

        val lastBlock = blockService.getByHash(blockData3.block.hash)

        val calculatedLIB = blockService.calculateLIBForBlock(lastBlock.hash)

        assertEquals(firstBlock.hash, calculatedLIB.libHash)
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

        val blockBranch1 = blockProcessor.createNextBlock(firstBlock, validatorForRound(2), 2)
        assertNotNull(blockBranch1.transactions.find { it.hash == tx.hash })

        val blockBranch2 = blockProcessor.createNextBlock(firstBlock, validatorForRound(2), 2)
        assertNotNull(blockBranch2.transactions.find { it.hash == tx.hash })

        val txs1 = blockProcessor.getPendingTransactionsByLastBlock(blockBranch1.block.toModel())
        assertEquals(1, txs1.size)

        val txs2 = blockProcessor.getPendingTransactionsByLastBlock(blockBranch2.block.toModel())
        assertEquals(1, txs2.size)
    }

    @Test
    fun testGetBlockRange() {
        populateBlocks(cleanup = false)

        val blocksRange = blockProcessor.getBlocksRange(
            blocks.first().block.toModel(),
            blocks.last().block.toModel()
        )

        val blockHashes = blocks.drop(1).map { it.block.hash }
        val rangeBlockHashes = blocksRange.map { it.hash }

        assertEquals(blockHashes, rangeBlockHashes)
    }

    @Test
    fun testVerifyBlock() {
        val blockData = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)
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

        val votes = blocks[0].votes.map { voteVO ->
            val account = accountService.ensureExistsAndGetByPublicKey(voteVO.publicKey)
            voteVO.toModel(account)
        }

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

        val blockData = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(9), 9)

        assertEquals(txCount, blockData.transactions.size)
    }

    @Test
    fun testRejectsBlockWithTheSameHash() {
        populateBlocks()

        val blockData = blocks[0]

        assertTrue(blockService.isAcceptable(blockData.block.toModel()))

        TimeUtils.setTestRound(blockData.block.round)

        eventReceiverService.onBlocks(listOf(blockData))

        assertFalse(blockService.isAcceptable(blockData.block.toModel()))
    }

    @Test
    fun testInvalidTransactionNotIncluded() {

        testService.createTransaction()
        testService.createInvalidTransaction()

        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        assertEquals(1, block.transactions.size)
    }

    @Test
    fun testPropertyAppliedWhenLIBChanged() {
        populateBlocks()

        assertNull(propertyService.findById(propertyId))

        TimeUtils.setTestRound(blocks[0].block.round)
        eventReceiverService.onBlocks(listOf(blocks[0]))

        assertNull(propertyService.findById(propertyId))
        val block0 = blockService.loadBlockByHash(blocks[0].block.hash)
        assertEquals(1, block0.transactions.size)

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        TimeUtils.setTestRound(blocks[1].block.round)
        eventReceiverService.onBlocks(listOf(blocks[1]))

        assertNull(propertyService.findById(propertyId))
        val block1 = blockService.loadBlockByHash(blocks[1].block.hash)
        assertEquals(1, block1.transactions.size)

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        TimeUtils.setTestRound(blocks[2].block.round)
        eventReceiverService.onBlocks(listOf(blocks[2]))

        assertNull(propertyService.findById(propertyId))
        val block2 = blockService.loadBlockByHash(blocks[2].block.hash)
        assertEquals(1, block2.transactions.size)

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        TimeUtils.setTestRound(blocks[3].block.round)
        eventReceiverService.onBlocks(listOf(blocks[3]))

        val block3 = blockService.loadBlockByHash(blocks[3].block.hash)
        assertEquals(1, block3.transactions.size)

        assertPropertyValueCandidate("4")
        assertPropertyValue("1")

        TimeUtils.setTestRound(blocks[4].block.round)
        eventReceiverService.onBlocks(listOf(blocks[4]))

        val block4 = blockService.loadBlockByHash(blocks[4].block.hash)
        assertEquals(1, block4.transactions.size)

        assertPropertyValueCandidate("5")
        assertPropertyValue("2")
    }

    private fun populateBlocks(cleanup: Boolean = true) {

        val space = spaceService.getById(DEFAULT_SPACE)

        txs[0] = testService.createTransaction()
        blocks.add(0, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1))
        assertEquals(blocks[0].block.producerId, accountService.getProducerBySpace(space, 1).accountId)
        assertEquals(firstBlock.hash, blocks[0].block.libHash)
        assertEquals(firstBlock.hash, txs[0]!!.referencedBlockHash)

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        txs[1] = testService.createTransaction()
        blocks.add(1, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2))

        assertEquals(txs[1]!!.hash, blocks[1].transactions.first().hash)
        assertEquals(1, blocks[1].transactions.size)

        assertEquals(firstBlock.hash, blocks[1].block.libHash)
        assertEquals(firstBlock.hash, txs[1]!!.referencedBlockHash)

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        txs[2] = testService.createTransaction()
        blocks.add(2, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3))

        assertEquals(txs[2]!!.hash, blocks[2].transactions.first().hash)
        assertEquals(1, blocks[2].transactions.size)

        assertEquals(firstBlock.hash, blocks[2].block.libHash)
        assertEquals(firstBlock.hash, txs[2]!!.referencedBlockHash)

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        txs[3] = testService.createTransaction() // lib is first block yet
        blocks.add(3, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(4), 4))

        assertEquals(txs[3]!!.hash, blocks[3].transactions.first().hash)
        assertEquals(1, blocks[3].transactions.size)

        // lib changed, for the first time
        // all transactions from block[0] must be applied
        assertEquals(blocks[0].block.hash, blocks[3].block.libHash)
        assertEquals(firstBlock.hash, txs[3]!!.referencedBlockHash)

        assertPropertyValueCandidate("4")
        assertPropertyValue("1")

        txs[4] = testService.createTransaction()
        blocks.add(4, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(5), 5))

        assertEquals(txs[4]!!.hash, blocks[4].transactions.first().hash)
        assertEquals(1, blocks[4].transactions.size)

        assertEquals(blocks[1].block.hash, blocks[4].block.libHash)
        assertEquals(blocks[0].block.hash, txs[4]!!.referencedBlockHash)

        assertPropertyValueCandidate("5")
        assertPropertyValue("2")

        txs[5] = testService.createTransaction()
        blocks.add(5, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(6), 6))

        assertEquals(txs[5]!!.hash, blocks[5].transactions.first().hash)
        assertEquals(1, blocks[5].transactions.size)

        assertEquals(blocks[2].block.hash, blocks[5].block.libHash)
        assertEquals(blocks[1].block.hash, txs[5]!!.referencedBlockHash)

        assertPropertyValueCandidate("6")
        assertPropertyValue("3")

        txs[6] = testService.createTransaction()
        blocks.add(6, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(7), 7))

        assertEquals(txs[6]!!.hash, blocks[6].transactions.first().hash)
        assertEquals(1, blocks[6].transactions.size)

        assertEquals(blocks[3].block.hash, blocks[6].block.libHash)
        assertEquals(blocks[2].block.hash, txs[6]!!.referencedBlockHash)

        assertPropertyValueCandidate("7")
        assertPropertyValue("4")

        txs[7] = testService.createTransaction()
        blocks.add(7, blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(8), 8))

        assertEquals(txs[7]!!.hash, blocks[7].transactions.first().hash)
        assertEquals(1, blocks[7].transactions.size)

        assertEquals(blocks[4].block.hash, blocks[7].block.libHash)
        assertEquals(blocks[3].block.hash, txs[7]!!.referencedBlockHash)

        assertPropertyValueCandidate("8")
        assertPropertyValue("5")

        if (cleanup) {
            val genesis = genesisService.exportFirst(DEFAULT_SPACE)
            testService.cleanup()
            testService.importAccounts()
            genesisService.importFirst(genesis)
        }
    }
}