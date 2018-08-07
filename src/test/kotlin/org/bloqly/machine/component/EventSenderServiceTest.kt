package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.NodeRepository
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class, EventSenderServiceTest.TestConfiguration::class])
class EventSenderServiceTest : BaseTest() {

    @Configuration
    class TestConfiguration {

        @Bean
        @Primary
        fun getRestTemplate(): RestTemplate {
            return Mockito.mock(RestTemplate::class.java)
        }
    }

    @Autowired
    private lateinit var executorService: ExecutorService

    @Autowired
    private lateinit var eventSenderService: EventSenderService

    @Autowired
    private lateinit var eventReceiverService: EventReceiverService

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private val node = Node(id = NodeId("127.0.0.1", 8080), addedTime = Instant.now().toEpochMilli())

    @Before
    override fun setup() {
        super.setup()

        Mockito.clearInvocations(restTemplate)

        nodeRepository.save(node)
    }

    @Test
    fun testSendProposalsNoNodes() {

        nodeRepository.deleteAll()

        eventSenderService.sendProposals(getProposals())
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendProposals() {
        eventReceiverService.receiveVotes(testService.getVotes())

        val path = APIUtils.getEventPath(node, "blocks")

        val proposals = getProposals()

        val entity = HttpEntity(BlockDataList(proposals))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendProposals(proposals)
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    @Test
    fun testSendProposalsFailed() {
        eventReceiverService.receiveVotes(testService.getVotes())

        val path = APIUtils.getEventPath(node, "blocks")

        val proposals = getProposals()

        val entity = HttpEntity(BlockDataList(proposals))

        val response = ResponseEntity<String>(REQUEST_TIMEOUT)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendProposals(proposals)
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    @Test
    fun testSendVotesNoNodes() {

        nodeRepository.deleteAll()

        eventSenderService.sendVotes(getVotes())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendVotes() {

        val path = APIUtils.getEventPath(node, "votes")

        val votes = getVotes()

        val entity = HttpEntity(VoteList.fromVotes(votes))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendVotes(votes)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    @Test
    fun testSendVotesFailed() {

        val path = APIUtils.getEventPath(node, "votes")

        val votes = getVotes()

        val entity = HttpEntity(VoteList.fromVotes(votes))

        val response = ResponseEntity<String>(REQUEST_TIMEOUT)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendVotes(votes)
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    @Test
    fun testSendVotesAlreadySent() {
        val votes = getVotes()

        assertEquals(4, votes.size)

        eventSenderService.sendVotes(votes)

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendTransactionsNoNodes() {

        nodeRepository.deleteAll()

        eventSenderService.sendTransactions(getTransactions())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendTransactions() {

        val path = APIUtils.getEventPath(node, "transactions")

        val transactions = getTransactions()

        val entity = HttpEntity(TransactionList.fromTransactions(transactions))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendTransactions(transactions)

        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    @Test
    fun testSendTransactionsFailed() {

        val path = APIUtils.getEventPath(node, "transactions")

        val transactions = getTransactions()

        val entity = HttpEntity(TransactionList.fromTransactions(transactions))

        val response = ResponseEntity<String>(REQUEST_TIMEOUT)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        eventSenderService.sendTransactions(transactions)
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)
    }

    private fun getTransactions(): List<Transaction> = listOf(testService.createTransaction())

    private fun getVotes(): List<Vote> = eventProcessorService.onGetVotes()

    private fun getProposals(): List<BlockData> = eventProcessorService.onProduceBlock()
}