package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockServiceTest : BaseTest() {

    @Test
    fun testFirstBlockIsFinal() {
        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)
        assertEquals(0, lastBlock.height)

        assertEquals(lastBlock, getLIB())
    }

    @Test
    fun testFirstBlockIsAfterLIB() {
        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        assertEquals(0, lastBlock.height)
        assertTrue(blockService.isAfterLIB(lastBlock))
    }

    @Test
    fun testNextBlockIsAfterLIB() {
        val blockData = createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        assertTrue(blockService.isAfterLIB(blockData.toModel()))
    }

    @Test
    fun testNotAfterLIB() {
        val blocks = listOf(
            createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1),
            createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3),
            createNextBlock(DEFAULT_SPACE, validatorForRound(4), 4),
            createNextBlock(DEFAULT_SPACE, validatorForRound(5), 5),
            createNextBlock(DEFAULT_SPACE, validatorForRound(6), 6),
            createNextBlock(DEFAULT_SPACE, validatorForRound(7), 7),
            createNextBlock(DEFAULT_SPACE, validatorForRound(8), 8),
            createNextBlock(DEFAULT_SPACE, validatorForRound(9), 9)
        )

        assertEquals(2, blocks.last().block.libHeight)

        try {
            createNextBlock(blocks[0].toModel(), validatorForRound(2), 2)
            fail()
        } catch (e: Exception) {

        }
    }

    @Test
    fun testVerticalFinality() {
        val block0 = blockService.getLastBlockBySpace(DEFAULT_SPACE)
        assertEquals(0, block0.height)

        assertEquals(block0.hash, getLIB().hash)

        val block1 = createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block1.libHeight)

        val block2 = createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block2.libHeight)

        val block3 = createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block3.libHeight)

        val block4 = createNextBlock(DEFAULT_SPACE, validatorForRound(4), 4).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block4.libHeight)

        val block5 = createNextBlock(DEFAULT_SPACE, validatorForRound(5), 5).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block5.libHeight)

        val block6 = createNextBlock(DEFAULT_SPACE, validatorForRound(6), 6).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block6.libHeight)

        // same proposer, nothing changed
        val block7 = createNextBlock(DEFAULT_SPACE, validatorForRound(10), 10).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.height, block7.libHeight)

        // change validator, continue changing LIB
        val block8 = createNextBlock(DEFAULT_SPACE, validatorForRound(11), 11).block
        assertEquals(block1.hash, getLIB().hash)
        assertEquals(block1.height, block8.libHeight)
    }

    @Test
    fun testHyperFinalization() {
        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        eventProcessorService.onGetVotes()
        createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)

        eventProcessorService.onGetVotes()
        val block = createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)

        assertEquals(1, block.block.libHeight)
    }

    @Test
    fun testHyperFinalizationNoPrevQuorum() {

        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        val votes1 = eventProcessorService.onGetVotes().filter { it.height == 1L }
        assertEquals(3, votes1.size)
        voteRepository.delete(votes1.first())
        val blockData1 = createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)
        assertEquals(1, blockData1.votes.size)

        val votes2 = eventProcessorService.onGetVotes().filter { it.height == 2L }
        assertEquals(3, votes2.size)
        val blockData2 = createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)
        assertEquals(2, blockData2.votes.size)

        assertEquals(0, blockData2.block.libHeight)
    }

    @Test
    fun testHyperFinalizationNoCurrQuorum() {
        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        val votes1 = eventProcessorService.onGetVotes().filter { it.height == 1L }
        assertEquals(3, votes1.size)
        val blockData1 = createNextBlock(DEFAULT_SPACE, validatorForRound(2), 2)
        assertEquals(2, blockData1.votes.size)

        val votes2 = eventProcessorService.onGetVotes().filter { it.height == 2L }
        assertEquals(3, votes2.size)
        voteRepository.delete(votes2.first())
        val blockData2 = createNextBlock(DEFAULT_SPACE, validatorForRound(3), 3)
        assertEquals(1, blockData2.votes.size)

        assertEquals(0, blockData2.block.libHeight)
    }
}