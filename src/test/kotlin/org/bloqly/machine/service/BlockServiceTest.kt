package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Block
import org.bloqly.machine.test.TestService
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockServiceTest {

    @Autowired
    private lateinit var blockService: BlockService

    @Autowired
    private lateinit var testService: TestService

    @Before
    fun setup() {
        testService.cleanup()
        testService.createBlockchain()
    }

    @Test
    fun testFirstBlockIsFinal() {
        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)
        assertEquals(0, lastBlock.height)

        assertEquals(lastBlock, getLIB())
    }

    @Test
    fun testVerticalFinality() {
        val block0 = blockService.getLastBlockForSpace(DEFAULT_SPACE)
        assertEquals(0, block0.height)

        assertEquals(block0, getLIB())

        val block1 = testService.createBlock(block0.hash, block0.height, "proposer1")
        assertEquals(block0, getLIB())

        val block2 = testService.createBlock(block1.hash, block1.height, "proposer2")
        assertEquals(block0, getLIB())

        val block3 = testService.createBlock(block2.hash, block2.height, "proposer3")
        assertEquals(block0, getLIB())

        val block4 = testService.createBlock(block3.hash, block3.height, "proposer4")
        // now 3 out of 4 validators have built on block1, it is final now
        assertEquals(block1, getLIB())

        val block5 = testService.createBlock(block4.hash, block4.height, "proposer1")
        assertEquals(block2, getLIB())

        val block6 = testService.createBlock(block5.hash, block5.height, "proposer2")
        assertEquals(block3, getLIB())

        // same proposer, nothing changed
        val block7 = testService.createBlock(block6.hash, block6.height, "proposer2")
        assertEquals(block3, getLIB())

        // change validator, continue changing LIB
        testService.createBlock(block7.hash, block7.height, "proposer3")
        assertEquals(block4, getLIB())
    }

    private fun getLIB(): Block = blockService.getLIBForSpace(DEFAULT_SPACE)
}