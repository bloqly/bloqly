package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Block
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.encode16
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
    private lateinit var blockRepository: BlockRepository

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

        val block1 = createBlock(block0.hash, block0.height, "proposer1")
        assertEquals(block0, getLIB())

        val block2 = createBlock(block1.hash, block1.height, "proposer2")
        assertEquals(block0, getLIB())

        val block3 = createBlock(block2.hash, block2.height, "proposer3")
        assertEquals(block0, getLIB())

        val block4 = createBlock(block3.hash, block3.height, "proposer4")
        // now 3 out of 4 validators have built on block1, it is final now
        assertEquals(block1, getLIB())

        val block5 = createBlock(block4.hash, block4.height, "proposer1")
        assertEquals(block2, getLIB())

        val block6 = createBlock(block5.hash, block5.height, "proposer2")
        assertEquals(block3, getLIB())
    }

    private fun getLIB(): Block = blockService.getLIBForSpace(DEFAULT_SPACE)

    private fun createBlock(parentHash: String, height: Long, proposerId: String): Block {

        return blockRepository.save(
            Block(
                spaceId = DEFAULT_SPACE,
                height = height + 1,
                weight = 0,
                diff = 0,
                round = 0,
                timestamp = 0,
                parentHash = parentHash,
                proposerId = proposerId,
                validatorTxHash = byteArrayOf(),
                signature = byteArrayOf(),
                hash = CryptoUtils.hash(parentHash.toByteArray()).encode16()
            )
        )
    }
}