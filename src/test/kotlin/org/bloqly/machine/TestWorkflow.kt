package org.bloqly.machine

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventSenderService
import org.bloqly.machine.component.SerializationService
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
    private lateinit var eventSenderService: EventSenderService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var serializationService: SerializationService

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

        eventSenderService.sendTransactions(
                TransactionListVO(transactions = listOf(transactionVO))
        )

        sendVotes()

        sendProposals()

        selectBestProposal()

        // END

        val lastBlock = blockReceiverService.findFirstBySpaceOrderByHeightDesc(DEFAULT_SPACE)

        assertEquals(1, lastBlock.height)
    }

    private fun sendVotes() {

        val votes = eventProcessorService.onGetVote().map { serializationService.voteToVO(it) }

        eventSenderService.sendVotes(votes)
    }

    private fun sendProposals() {
        val proposals = eventProcessorService.onGetProposals().map { serializationService.blockDataToVO(it) }

        eventSenderService.sendProposals(proposals)
    }

    private fun selectBestProposal() {

        val bestProposals = eventProcessorService.onSelectBestProposal()

        assertEquals(1, bestProposals.size)
    }
}