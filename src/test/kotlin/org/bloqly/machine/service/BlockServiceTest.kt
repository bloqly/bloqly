package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.vo.BlockData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockServiceTest : BaseTest() {

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

        assertEquals(block0.hash, getLIB().hash)

        val block1 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.hash, block1.libHash)

        val block2 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.hash, block2.libHash)

        val block3 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(2), passphrase(2), 3).block
        assertEquals(block0.hash, getLIB().hash)
        assertEquals(block0.hash, block3.libHash)

        val block4 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(3), passphrase(3), 4).block
        // now 3 out of 4 validators have built on block1, it is final now
        assertEquals(block1.hash, getLIB().hash)
        assertEquals(block1.hash, block4.libHash)

        val block5 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 5).block
        assertEquals(block2.hash, getLIB().hash)
        assertEquals(block2.hash, block5.libHash)

        val block6 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 6).block
        assertEquals(block3.hash, getLIB().hash)
        assertEquals(block3.hash, block6.libHash)

        // same proposer, nothing changed
        val block7 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 7).block
        assertEquals(block3.hash, getLIB().hash)
        assertEquals(block3.hash, block7.libHash)

        // change validator, continue changing LIB
        val block8 = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(2), passphrase(2), 8).block
        assertEquals(block4.hash, getLIB().hash)
        assertEquals(block4.hash, block8.libHash)
    }

    @Test
    fun testHyperFinalization() {
        eventProcessorService.onGetVotes()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        eventProcessorService.onGetVotes()
        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertTrue(isHyperFinalizer(block))
    }

    @Test
    fun testHyperFinalizationNoPrevQuorum() {
        val votes1 = eventProcessorService.onGetVotes()
        voteRepository.deleteAll(votes1.sortedBy { it.validator.accountId }.takeLast(2))

        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        eventProcessorService.onGetVotes()
        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertFalse(isHyperFinalizer(block))
    }

    @Test
    fun testHyperFinalizationNoCurrQuorum() {
        eventProcessorService.onGetVotes()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        val votes2 = eventProcessorService.onGetVotes()
        voteRepository.deleteAll(votes2.sortedBy { it.validator.accountId }.takeLast(2))

        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertFalse(isHyperFinalizer(block))
    }

    @Test
    fun testHyperFinalizationExactPrevQuorum() {
        val votes1 = eventProcessorService.onGetVotes()
        voteRepository.deleteAll(votes1.take(1))

        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        eventProcessorService.onGetVotes()
        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertTrue(isHyperFinalizer(block))
    }

    @Test
    fun testHyperFinalizationExactCurrQuorum() {
        eventProcessorService.onGetVotes()
        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        val votes2 = eventProcessorService.onGetVotes()
        voteRepository.deleteAll(votes2.take(1))

        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertTrue(isHyperFinalizer(block))
    }

    @Test
    fun testHyperFinalizationNotSameValidatorsQuorum() {
        val votes1 = eventProcessorService.onGetVotes()
        val first = votes1.sortedBy { it.validator.accountId }.first()
        voteRepository.delete(first)
        blockProcessor.createNextBlock(DEFAULT_SPACE, validator(0), passphrase(0), 1)

        val votes2 = eventProcessorService.onGetVotes()
        val last = votes2.sortedBy { it.validator.accountId }.last()
        voteRepository.delete(last)
        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validator(1), passphrase(1), 2)

        assertNotEquals(first, last)
        assertFalse(isHyperFinalizer(block))
    }

    private fun isHyperFinalizer(blockData: BlockData): Boolean =
        blockService.isHyperFinalizer(blockService.loadBlockByHash(blockData.block.hash), 3)
}