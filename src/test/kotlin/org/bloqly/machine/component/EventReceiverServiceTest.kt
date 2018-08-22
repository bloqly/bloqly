package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
open class EventReceiverServiceTest : BaseTest() {

    @Test
    fun testReceiveNewTransactionsWithNewBlock() {

        val tx = testService.createTransaction()
        val block = blockProcessor.createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        val genesis = genesisService.exportFirst(DEFAULT_SPACE)

        testService.cleanup(deleteAccounts = false)

        assertFalse(transactionService.existsByHash(tx.hash))

        genesisService.importFirst(genesis)

        eventReceiverService.onBlocks(listOf(block))

        assertTrue(transactionService.existsByHash(tx.hash))
    }

    @Test
    fun testDoNotAcceptSameHeightFromDifferentValidators() {

        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)

        val block1 = blockProcessor.createNextBlock(lastBlock, validatorForRound(1), 1).block
        val block2 = blockProcessor.createNextBlock(lastBlock, validatorForRound(2), 2).block

        val validators = accountService.getValidatorsForSpaceId(DEFAULT_SPACE)

        val v1 = voteService.newVote(validators[0], passphrase(validators[0]), block1.toModel())
        val v2 = voteService.newVote(validators[0], passphrase(validators[0]), block2.toModel())

        assertEquals(v1.height, v2.height)

        eventReceiverService.receiveVotes(listOf(v1.toVO()))

        assertEquals(1, voteRepository.count())

        assertEquals(v1.validator.accountId, voteRepository.findAll().first().validator.accountId)

        objectFilterService.clear()

        eventReceiverService.receiveVotes(listOf(v2.toVO()))

        assertEquals(1, voteRepository.count())

        assertEquals(v1.validator.accountId, voteRepository.findAll().first().validator.accountId)
    }

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