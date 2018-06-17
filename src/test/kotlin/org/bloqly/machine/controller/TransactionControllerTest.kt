package org.bloqly.machine.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.TransactionList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class TransactionControllerTest {

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @LocalServerPort
    private var port: Int = 0

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()
    }

    @Test
    fun testReceiveTransactions() {

        val url = "http://localhost:$port/data/transactions"

        val transaction = testService.newTransaction()

        val transactionPayload = objectMapper.writeValueAsString(
            TransactionList.fromTransactions(listOf(transaction))
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity<String>(transactionPayload, headers)

        assertFalse(transactionRepository.existsById(transaction.id))

        restTemplate.postForObject(url, entity, Void.TYPE)

        assertTrue(transactionRepository.existsById(transaction.id))
    }
}