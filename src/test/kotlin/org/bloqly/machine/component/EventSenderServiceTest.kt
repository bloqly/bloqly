package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.EntityEvent
import org.bloqly.machine.model.EntityEventId
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.EntityEventRepository
import org.bloqly.machine.repository.NodeRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class, EventSenderServiceTest.TestConfiguration::class])
class EventSenderServiceTest {

    @Configuration
    class TestConfiguration {

        @Bean
        @Primary
        fun getRestTemplate(): RestTemplate {
            return Mockito.mock(RestTemplate::class.java)
        }
    }

    @Autowired
    private lateinit var eventSenderService: EventSenderService

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @Autowired
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var entityEventRepository: EntityEventRepository

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private val node = Node(id = NodeId("127.0.0.1", 8080), addedTime = Instant.now().toEpochMilli())

    @Before
    fun init() {
        testService.createBlockchain()
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testSendProposalsNoNodes() {

        eventSenderService.sendProposals(getProposals())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendProposals() {

        val path = "http://${node.id}/blocks"

        val proposals = getProposals()

        val eventId = EntityEventId(proposals.first().block.id, node.id.toString())

        assertFalse(entityEventRepository.existsById(eventId))

        val entity = HttpEntity(BlockDataList(proposals))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        nodeRepository.save(node)

        eventSenderService.sendProposals(proposals)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)

        assertTrue(entityEventRepository.existsById(eventId))
    }

    @Test
    fun testSendProposalsAlreadySent() {
        val proposals = getProposals()

        assertEquals(1, proposals.size)

        val events = proposals.map {
            EntityEvent(
                entityEventId = EntityEventId(it.block.id, node.id.toString()),
                timestamp = Instant.now().toEpochMilli()
            )
        }

        nodeRepository.save(node)

        entityEventRepository.saveAll(events)

        eventSenderService.sendProposals(proposals)

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendVotesNoNodes() {

        eventSenderService.sendVotes(getVotes())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendVotes() {

        val path = "http://${node.id}/votes"

        val votes = getVotes()

        val eventId = EntityEventId(votes.first().id.toString(), node.id.toString())

        assertFalse(entityEventRepository.existsById(eventId))

        val entity = HttpEntity(VoteList.fromVotes(votes))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        nodeRepository.save(node)

        eventSenderService.sendVotes(votes)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)

        assertTrue(entityEventRepository.existsById(eventId))
    }

    @Test
    fun testSendVotesAlreadySent() {
        val votes = getVotes()

        assertEquals(3, votes.size)

        val events = votes.map {
            EntityEvent(
                entityEventId = EntityEventId(it.id.toString(), node.id.toString()),
                timestamp = Instant.now().toEpochMilli()
            )
        }

        nodeRepository.save(node)

        entityEventRepository.saveAll(events)

        eventSenderService.sendVotes(votes)

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendTransactionsNoNodes() {

        eventSenderService.sendTransactions(getTransactions())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendTransactions() {

        val path = "http://${node.id}/transactions"

        val transactions = getTransactions()

        val eventId = EntityEventId(transactions.first().id, node.id.toString())

        assertFalse(entityEventRepository.existsById(eventId))

        val entity = HttpEntity(TransactionList.fromTransactions(transactions))

        val response = ResponseEntity<String>(OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, String::class.java))
            .thenReturn(response)

        nodeRepository.save(node)

        eventSenderService.sendTransactions(transactions)

        Mockito.verify(restTemplate).postForEntity(path, entity, String::class.java)

        assertTrue(entityEventRepository.existsById(eventId))
    }

    @Test
    fun testSendTransactionsAlreadySent() {
        val transactions = getTransactions()

        val eventId = EntityEventId(transactions.first().id, node.id.toString())

        nodeRepository.save(node)

        entityEventRepository.save(EntityEvent(eventId, Instant.now().toEpochMilli()))

        eventSenderService.sendTransactions(transactions)

        Mockito.verifyZeroInteractions(restTemplate)
    }

    private fun getTransactions(): List<Transaction> = listOf(testService.newTransaction())

    private fun getVotes(): List<Vote> = eventProcessorService.onGetVotes()

    private fun getProposals(): List<BlockData> = eventProcessorService.onGetProposals()
}