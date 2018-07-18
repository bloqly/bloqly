package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.TransactionOutputId
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.vo.BlockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockProcessorTest {

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var transactionOutputRepository: TransactionOutputRepository

    @Autowired
    private lateinit var propertyService: PropertyService

    @Autowired
    private lateinit var genesisService: GenesisService

    @Autowired
    private lateinit var blockProcessor: BlockProcessor

    @Autowired
    private lateinit var blockService: BlockService

    @Autowired
    private lateinit var blockRepository: BlockRepository

    @Autowired
    private lateinit var testService: TestService

    private val blocks = mutableListOf<BlockData>()

    private lateinit var firstBlock: Block

    private lateinit var propertyId: PropertyId

    /**
     * In this test we send 1 amount to a user in each blocks and check the actual property values
     * are in tact with block finality logic
     */
    @Before
    fun setup() {

        propertyId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, testService.getUser().accountId, "balance")

        testService.cleanup()
        testService.createBlockchain()

        firstBlock = blockService.getLIBForSpace(Application.DEFAULT_SPACE)
        assertEquals(firstBlock.hash, getLIB().hash)
        assertEquals(0, firstBlock.height)

        val tx0 = testService.createTransaction()
        blocks.add(0, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(0), 1))
        assertEquals(firstBlock.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[0], firstBlock.hash)

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        val tx1 = testService.createTransaction()
        blocks.add(1, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(1), 2))
        assertEquals(firstBlock.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[1], firstBlock.hash)

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        val tx2 = testService.createTransaction()
        blocks.add(2, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(2), 3))
        assertEquals(firstBlock.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[2], firstBlock.hash)

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        val tx3 = testService.createTransaction() // lib is first block yet
        blocks.add(3, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(3), 4))
        // lib changed, for the first time
        // all transactions from block[0] must be applied
        assertEquals(blocks[0].block.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[3], firstBlock.hash)

        assertPropertyValueCandidate("4")
        assertPropertyValue("1")

        val tx4 = testService.createTransaction()
        blocks.add(4, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(0), 5))
        assertEquals(blocks[1].block.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[4], blocks[0].block.hash)

        assertPropertyValueCandidate("5")
        assertPropertyValue("2")

        val tx5 = testService.createTransaction()
        blocks.add(5, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(1), 6))
        assertEquals(blocks[2].block.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[5], blocks[1].block.hash)

        assertPropertyValueCandidate("6")
        assertPropertyValue("3")

        val tx6 = testService.createTransaction()
        blocks.add(6, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(2), 7))
        assertEquals(blocks[3].block.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[6], blocks[2].block.hash)

        assertPropertyValueCandidate("7")
        assertPropertyValue("4")

        val tx7 = testService.createTransaction()
        blocks.add(7, blockProcessor.createNextBlock(Application.DEFAULT_SPACE, testService.getValidator(3), 8))
        assertEquals(blocks[4].block.hash, getLIB().hash)
        assertTxReferencesBlock(blocks[7], blocks[3].block.hash)

        assertPropertyValueCandidate("8")
        assertPropertyValue("5")

        val genesis = genesisService.exportFirst(DEFAULT_SPACE)
        testService.cleanup()
        genesisService.importFirst(genesis)

        firstBlock = blockService.getLIBForSpace(Application.DEFAULT_SPACE)
        assertEquals(0, firstBlock.height)
    }

    @Test
    fun testBlockProcessed() {

        assertNull(propertyService.findById(propertyId))

        blockProcessor.processReceivedBlock(blocks[0])
        assertNotNull(blockRepository.findByHash(blocks[0].block.hash))

        assertNull(propertyService.findById(propertyId))
    }

    @Test
    fun testPropertyAppliedWhenLIBChanged() {
        assertNull(propertyService.findById(propertyId))

        blockProcessor.processReceivedBlock(blocks[0])
        assertNull(propertyService.findById(propertyId))
        val block0 = blockService.loadBlockByHash(blocks[0].block.hash)
        assertEquals(1, block0.transactions.size)

        assertPropertyValueCandidate("1")
        assertNoPropertyValue()

        blockProcessor.processReceivedBlock(blocks[1])
        assertNull(propertyService.findById(propertyId))
        val block1 = blockService.loadBlockByHash(blocks[1].block.hash)
        assertEquals(1, block1.transactions.size)

        assertPropertyValueCandidate("2")
        assertNoPropertyValue()

        blockProcessor.processReceivedBlock(blocks[2])
        assertNull(propertyService.findById(propertyId))
        val block2 = blockService.loadBlockByHash(blocks[2].block.hash)
        assertEquals(1, block2.transactions.size)

        assertPropertyValueCandidate("3")
        assertNoPropertyValue()

        blockProcessor.processReceivedBlock(blocks[3])
        val block3 = blockService.loadBlockByHash(blocks[3].block.hash)
        assertEquals(1, block3.transactions.size)

        assertPropertyValueCandidate("4")
        assertPropertyValue("1")

        blockProcessor.processReceivedBlock(blocks[4])
        val block4 = blockService.loadBlockByHash(blocks[4].block.hash)
        assertEquals(1, block4.transactions.size)

        assertPropertyValueCandidate("5")
        assertPropertyValue("2")
    }

    private fun assertPropertyValue(value: String) {
        val property: Property = propertyService.findById(propertyId)!!
        assertEquals(BInteger(value), ParameterUtils.readValue(property.value))
    }

    private fun assertNoPropertyValue() {
        assertNull(propertyService.findById(propertyId))
    }

    private fun assertTxReferencesBlock(blockData: BlockData, blockHash: String) {
        assertTrue(blockData.transactions.count { it.referencedBlockHash == blockHash } == 1)
    }

    private fun getLIB(): Block {
        return blockService.getLIBForSpace(DEFAULT_SPACE)
    }

    private fun assertPropertyValueCandidate(value: String) {
        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)
        val tx = blockService.loadBlockByHash(lastBlock.hash).transactions.first()

        val txOutput = transactionOutputRepository.findById(TransactionOutputId(lastBlock.hash, tx.hash)).orElseThrow()

        val properties = ObjectUtils.readProperties(txOutput.output)

        val property = properties.first { it.id == propertyId }

        assertEquals(BInteger(value), ParameterUtils.readValue(property.value))
    }
}