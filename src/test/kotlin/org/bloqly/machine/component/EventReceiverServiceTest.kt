package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
open class EventReceiverServiceTest : BaseTest() {

    @Test
    fun testAccountPublicKeyPopulatedWhenReceiveVotes() {

        val votes = eventProcessorService.onGetVotes()

        val votesVOs = votes.map { it.toVO() }

        voteRepository.deleteAll()
        accountRepository.deleteAll()

        eventReceiverService.receiveVotes(votesVOs)

        assertEquals(4, accountRepository.count())
    }

    @Test
    fun testChainsJoinAfterSplit() {

        val blockChain1 = arrayListOf(
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(1), 1),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(2), 2),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(5), 5),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(6), 6),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(9), 9),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(10), 10)
        )

        val genesis = genesisService.exportFirst(DEFAULT_SPACE)

        testService.cleanup(deleteAccounts = false)

        genesisService.importFirst(genesis)

        val blockChain2 = arrayListOf(
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(3), 3),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(4), 4),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(7), 7),
            blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validatorForRound(8), 8)
        )

        assertEquals(blockChain1[0].block.parentHash, blockChain2[0].block.parentHash)

        eventReceiverService.onBlocks(blockChain1)

        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)

        assertEquals(blockChain1.last().block.hash, lastBlock.hash)
    }
}