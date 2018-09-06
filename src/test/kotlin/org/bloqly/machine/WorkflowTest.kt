package org.bloqly.machine

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.service.DeltaService
import org.bloqly.machine.test.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class WorkflowTest : BaseTest() {

    @Autowired
    private lateinit var deltaService: DeltaService

    @Test
    fun testNoDelta() {

        sendVotes()

        val deltas = deltaService.getDeltas()

        assertTrue(deltas.isEmpty())
    }

    @Test
    fun testProposalWithoutQuorum() {

        val votes = testService.getVotes()
        voteRepository.deleteAll()

        eventReceiverService.receiveVotes(votes.subList(0, 1))

        val proposals = eventProcessorService.onProduceBlock()

        assertTrue(proposals.isNotEmpty())
    }

    @Test
    fun testSingleRound() {

        val transaction = testService.createTransaction()

        sendTransactions(listOf(transaction))
        val pendingTxs = blockProcessor.getPendingTransactions()
        assertEquals(1, pendingTxs.size)

        sendVotes()
        assertEquals(4, voteRepository.findAll().toList().size)

        produceAndSendBlocks()

        assertEquals(1, getHeight())
    }

    private fun getHeight(): Long {
        return blockService.getLastBlockBySpace(DEFAULT_SPACE).height
    }

    private fun sendTransactions(transactions: List<Transaction>) {
        eventReceiverService.receiveTransactions(transactions.map { it.toVO() })
    }

    private fun sendVotes() {
        val votes = testService.getVotes()

        assertEquals(4, votes.size)

        eventReceiverService.receiveVotes(votes)
    }

    private fun produceAndSendBlocks() {
        val blocks = eventProcessorService.onProduceBlock()
        assertEquals(1, blocks.size)

        val blockData = blocks.first()

        // remove blocks, transactions and votes
        val block = blockRepository.findAll().first { it.height == 1L }

        assertEquals(1, blockData.transactions.size)
        assertTrue(blockData.transactions.all { transactionRepository.existsByHash(it.hash) })
        assertEquals(4, voteRepository.count())

        blockRepository.delete(block)

        assertEquals(1, blockData.transactions.size)
        assertTrue(blockData.transactions.all { transactionRepository.existsByHash(it.hash) })
        assertEquals(4, voteRepository.count())

        testService.cleanupBlockTransactions()
        finalizedTransactionRepository.deleteAll()
        transactionRepository.deleteAll()
        transactionOutputRepository.deleteAll()

        voteRepository.deleteAll()
        // need to do it otherwise the same votes won't be processed
        objectFilterService.clear()

        eventReceiverService.onBlocks(blocks)
        assertEquals(1, blockData.transactions.size)
        // votes are re-imported
        assertEquals(3, voteRepository.count())
    }
}