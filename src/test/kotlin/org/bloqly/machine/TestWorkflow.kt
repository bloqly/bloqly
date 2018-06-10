package org.bloqly.machine

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.TransactionListVO
import org.junit.After
import org.junit.Assert.assertEquals
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
    private lateinit var eventProcessorService: EventProcessorService

    @Autowired
    private lateinit var eventReceiverService: EventReceiverService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var blockReceiverService: BlockRepository

    @Before
    fun init() {
        testService.createBlockchain()
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testSingleRound() {

        val transactionVO = testService.newTransaction()

        sendTransactions(TransactionListVO(listOf(transactionVO)))

        sendVotes()

        sendProposals()

        selectBestProposal()

        // END

        val lastBlock = blockReceiverService.findFirstBySpaceOrderByHeightDesc(DEFAULT_SPACE)

        assertEquals(1, lastBlock.height)
    }

    private fun sendTransactions(transactionListVO: TransactionListVO) {
        eventReceiverService.receiveTransactions(transactionListVO.transactions)
    }

    private fun sendVotes() {

        val votes = eventProcessorService.onGetVote().map { it.toVO() }

        eventReceiverService.receiveVotes(votes)
    }

    private fun sendProposals() {
        val proposals = eventProcessorService.onGetProposals().map { it.toVO() }

        eventReceiverService.receiveProposals(proposals)
    }

    private fun selectBestProposal() {

        val bestProposals = eventProcessorService.onSelectBestProposal()

        assertEquals(1, bestProposals.size)
    }
}