package org.bloqly.machine.controller

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.TransactionVO
import org.junit.Assert.assertEquals
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
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var transactionService: TransactionService

    @LocalServerPort
    private var port: Int = 0

    private lateinit var node: Node

    private val headers = HttpHeaders()

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        headers.contentType = MediaType.APPLICATION_JSON

        node = Node(NodeId("localhost", port), 0)
    }

    @Test
    fun testReceiveTransactions() {

        val url = APIUtils.getDataPath(node, "transactions")

        val transaction = testService.createTransaction()

        val transactionPayload = ObjectUtils.writeValueAsString(
            TransactionList.fromTransactions(listOf(transaction))
        )

        val entity = HttpEntity(transactionPayload, headers)

        restTemplate.postForObject(url, entity, String::class.java)

        assertTrue(transactionRepository.existsByHash(transaction.hash))
    }

    @Test
    fun testUserCreateTransaction() {
        val url = APIUtils.getDataPath(node, "transactions") + "/new"

        // TODO rename "contract" function name to "main" everywhere
        val transactionRequestPayload = """
            {
                "space": "$DEFAULT_SPACE",
                "origin": "${testService.getRoot().accountId}",
                "passphrase": "root password",
                "destination": "${testService.getUser().accountId}",
                "transactionType": "CALL",
                "self": "self",
                "key": "contract",
                "args": [
                    {
                        "type": "BIGINT",
                        "value": "100"
                    }
                ]
            }
        """.trimIndent()

        val entity = HttpEntity(transactionRequestPayload, headers)

        assertEquals(
            0,
            transactionService.getPendingTransactionsBySpace(
                DEFAULT_SPACE, MAX_REFERENCED_BLOCK_DEPTH
            ).size
        )

        val tx = restTemplate.postForObject(url, entity, TransactionVO::class.java)

        val pendingTransactions = transactionService.getPendingTransactionsBySpace(
            DEFAULT_SPACE, MAX_REFERENCED_BLOCK_DEPTH
        )
        assertEquals(1, pendingTransactions.size)

        assertEquals(tx, pendingTransactions.first().toVO())
    }
}