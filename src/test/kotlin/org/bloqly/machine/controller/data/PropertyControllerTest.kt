package org.bloqly.machine.controller.data

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyValue
import org.bloqly.machine.model.ValueType
import org.bloqly.machine.test.BaseControllerTest
import org.bloqly.machine.util.APIUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigInteger

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class PropertyControllerTest : BaseControllerTest() {

    @Test
    fun testGetLastProperty() {
        testService.createTransaction()

        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        val url = APIUtils.getDataPath(node, "properties/search")

        val userEntity = HttpEntity(getBalanceRequestPayload(testService.getUser()), headers)

        val userValue =
            restTemplate.postForObject(url, userEntity, PropertyValue::class.java)

        assertEquals("1", userValue.value)
        assertEquals(ValueType.BIGINT, userValue.type)

        val rootEntity = HttpEntity(getBalanceRequestPayload(testService.getRoot()), headers)

        val rootValue = restTemplate.postForObject(url, rootEntity, PropertyValue::class.java)

        assertEquals(maxSupply.minus(BigInteger("5")).toString(), rootValue.value)
        assertEquals(ValueType.BIGINT, rootValue.type)
    }

    @Test
    fun testGetFinalizedProperty() {
        testService.createTransaction()

        createNextBlock(DEFAULT_SPACE, validatorForRound(1), 1)

        val url = APIUtils.getDataPath(node, "properties/search")

        val userEntity = HttpEntity(getFinalizedBalanceRequestPayload(testService.getUser()), headers)

        val userValueEntity =
            restTemplate.postForEntity(url, userEntity, String::class.java)

        assertEquals(HttpStatus.NOT_FOUND, userValueEntity.statusCode)

        val rootEntity = HttpEntity(getFinalizedBalanceRequestPayload(testService.getRoot()), headers)

        val rootValue = restTemplate.postForObject(url, rootEntity, PropertyValue::class.java)

        assertEquals(maxSupply.minus(BigInteger("4")).toString(), rootValue.value)
        assertEquals(ValueType.BIGINT, rootValue.type)
    }

    private fun getBalanceRequestPayload(account: Account) =
        """
            {
                "key": "balance",
                "target": "${account.accountId}"
            }
        """.trimIndent()

    private fun getFinalizedBalanceRequestPayload(account: Account) =
        """
            {
                "key": "balance",
                "target": "${account.accountId}",
                "finalized": true
            }
        """.trimIndent()
}