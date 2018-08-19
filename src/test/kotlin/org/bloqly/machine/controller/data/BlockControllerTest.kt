package org.bloqly.machine.controller.data

import junit.framework.Assert.assertEquals
import org.bloqly.machine.Application
import org.bloqly.machine.test.BaseControllerTest
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.BlockRequest
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class BlockControllerTest : BaseControllerTest() {

    @Test
    fun testReceiveTransactions() {

        blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validator(0), passphrase(0), 1)
        blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validator(1), passphrase(1), 2)
        blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validator(2), passphrase(2), 3)
        blockProcessor.createNextBlock(Application.DEFAULT_SPACE, validator(3), passphrase(3), 4)

        val deltaPayload = ObjectUtils.writeValueAsString(
            BlockRequest(
                spaceId = "main",
                startHeight = 1,
                endHeight = 3
            )
        )

        val entity = HttpEntity(deltaPayload, headers)

        val url = APIUtils.getDataPath(node, "blocks/search")

        val blockList = restTemplate.postForObject(url, entity, BlockDataList::class.java)

        assertEquals(2, blockList.blocks.size)
    }
}