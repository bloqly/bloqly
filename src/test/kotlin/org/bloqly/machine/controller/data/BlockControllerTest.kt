package org.bloqly.machine.controller.data

import org.bloqly.machine.Application
import org.bloqly.machine.test.BaseControllerTest
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.block.BlockData
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.block.BlockRangeRequest
import org.bloqly.machine.vo.block.BlockRequest
import org.bloqly.machine.vo.block.BlockVO
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class BlockControllerTest : BaseControllerTest() {

    private lateinit var blocks: List<BlockData>

    override fun setup() {
        super.setup()
        blocks = arrayListOf(
            createNextBlock(Application.DEFAULT_SPACE, validatorForRound(1), 1),
            createNextBlock(Application.DEFAULT_SPACE, validatorForRound(2), 2),
            createNextBlock(Application.DEFAULT_SPACE, validatorForRound(3), 3),
            createNextBlock(Application.DEFAULT_SPACE, validatorForRound(4), 4)
        )
    }

    @Test
    fun testRequestDeltas() {

        val deltaPayload = ObjectUtils.writeValueAsString(
            BlockRangeRequest(
                spaceId = "main",
                startHeight = 1,
                endHeight = 3
            )
        )

        val entity = HttpEntity(deltaPayload, headers)

        val url = APIUtils.getDataPath(node.id.toString(), "blocks/search")

        val blockList = restTemplate.postForObject(url, entity, BlockDataList::class.java)

        assertEquals(2, blockList.blocks.size)
    }

    @Test
    fun testGetLast() {

        val lastPayload = ObjectUtils.writeValueAsString(
            BlockRequest(spaceId = "main")
        )

        val entity = HttpEntity(lastPayload, headers)

        val url = APIUtils.getDataPath(node.id.toString(), "blocks/last")

        val block = restTemplate.postForObject(url, entity, BlockVO::class.java)

        assertEquals(blocks[3].block.hash, block.hash)
    }

    @Test
    fun testGetLIB() {

        val libPayload = ObjectUtils.writeValueAsString(
            BlockRequest(spaceId = "main")
        )

        val entity = HttpEntity(libPayload, headers)

        val url = APIUtils.getDataPath(node.id.toString(), "blocks/lib")

        val block = restTemplate.postForObject(url, entity, BlockVO::class.java)

        assertEquals(blocks[3].block.libHeight, block.height)
    }
}