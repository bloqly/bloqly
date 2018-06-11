package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.EntityEventRepository
import org.bloqly.machine.repository.NodeRepository
import org.bloqly.machine.test.TestService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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

    private val node = Node(id = NodeId("127.0.0.1", 8080), addedTime = Instant.now().toEpochMilli())

    @Before
    fun init() {
        testService.createBlockchain()

        nodeRepository.save(node)
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testSendTransactions() {
        val tx = testService.newTransaction()

        eventSenderService.sendTransactions(listOf(tx))
    }
}