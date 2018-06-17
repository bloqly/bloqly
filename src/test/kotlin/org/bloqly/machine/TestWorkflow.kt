package org.bloqly.machine

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.DeltaService
import org.bloqly.machine.test.TestService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class TestWorkflow {

    @Autowired
    private lateinit var deltaService: DeltaService

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @Autowired
    private lateinit var eventReceiverService: EventReceiverService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var blockService: BlockService

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()
    }


    @Test
    fun testNoDelta() {

        sendVotes()

        val deltas = deltaService.getDeltas()

        assertTrue(deltas.isEmpty())
    }

    @Test
    fun testSingleRound() {

        val transaction = testService.newTransaction()

        sendTransactions(listOf(transaction))

        sendVotes()

        sendProposals()

        selectBestProposal()

        // END

        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)

        assertEquals(1, lastBlock.height)
    }

    private fun sendTransactions(transactions: List<Transaction>) {
        eventReceiverService.receiveTransactions(transactions.map { it.toVO() })
    }

    private fun sendVotes() {

        val votes = eventProcessorService.onGetVotes().map { it.toVO() }

        eventReceiverService.receiveVotes(votes)
    }

    private fun sendProposals() {
        val proposals = eventProcessorService.onGetProposals()

        eventReceiverService.receiveProposals(proposals)
    }

    private fun selectBestProposal() {

        val bestProposals = eventProcessorService.onSelectBestProposal()

        assertEquals(1, bestProposals.size)
    }
}