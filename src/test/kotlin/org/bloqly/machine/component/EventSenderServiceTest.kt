package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.EntityEvent
import org.bloqly.machine.model.EntityEventId
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.repository.EntityEventRepository
import org.bloqly.machine.repository.NodeRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.TransactionList
import org.junit.After
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
import org.springframework.http.HttpStatus
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
    private lateinit var nodeRepository: NodeRepository

    @Autowired
    private lateinit var entityEventRepository: EntityEventRepository

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private val node = Node(id = NodeId("127.0.0.1", 8080), addedTime = Instant.now().toEpochMilli())

    private val path = "http://${node.id}/transactions"

    @Before
    fun init() {
        testService.createBlockchain()
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testSendTransactionsNoNodes() {

        eventSenderService.sendTransactions(getTransactions())

        Mockito.verifyZeroInteractions(restTemplate)
    }

    @Test
    fun testSendTransactions() {

        val transactions = getTransactions()

        val eventId = EntityEventId(transactions.first().id, node.id.toString())

        assertFalse(entityEventRepository.existsById(eventId))

        val entity = HttpEntity(TransactionList.fromTransactions(transactions))

        val response = ResponseEntity<Void>(HttpStatus.OK)

        Mockito.`when`(restTemplate.postForEntity(path, entity, Void.TYPE))
            .thenReturn(response)

        nodeRepository.save(node)

        eventSenderService.sendTransactions(transactions)

        Mockito.verify(restTemplate).postForEntity(path, entity, Void.TYPE)

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
}