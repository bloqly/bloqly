package org.bloqly.machine.controller.data

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.BaseControllerTest
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.TransactionVO
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class TransactionControllerTest : BaseControllerTest() {

    @Test
    fun testUserCreateTransaction() {
        val url = APIUtils.getDataPath(node, "transactions")

        val transactionRequestPayload = """
            {
                "space": "$DEFAULT_SPACE",
                "origin": "${testService.getRoot().accountId}",
                "passphrase": "root password",
                "destination": "${testService.getUser().accountId}",
                "transactionType": "CALL",
                "self": "self",
                "key": "main",
                "args": [
                    {
                        "type": "BIGINT",
                        "value": "100"
                    }
                ]
            }
        """.trimIndent()

        val entity = HttpEntity(transactionRequestPayload, headers)

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        assertEquals(0, blockProcessor.getPendingTransactions(lastBlock).size)

        TimeUtils.testTick()
        val tx = restTemplate.postForObject(url, entity, TransactionVO::class.java)

        val pendingTransactions = blockProcessor.getPendingTransactions(lastBlock)

        assertEquals(1, pendingTransactions.size)
        assertEquals(pendingTransactions.first().toVO(), tx)
    }
}