package org.bloqly.machine.controller

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockDataList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
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
class BlockControllerTest {

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var blockRepository: BlockRepository

    @Autowired
    private lateinit var eventReceiverService: EventReceiverService

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @LocalServerPort
    private var port: Int = 0

    private lateinit var node: Node

    private lateinit var url: String

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        TimeUtils.setTestTime(0)

        node = Node(NodeId("localhost", port), 0)

        eventReceiverService.receiveVotes(testService.getVotes())

        url = APIUtils.getDataPath(node, "blocks")
    }

    @After
    fun tearDown() {
        TimeUtils.reset()
    }

    private fun getHttpEntity(): HttpEntity<String> {
        val blocks = eventProcessorService.onProduceBlock()

        assertTrue(blocks.isNotEmpty())

        val proposalsPayload = ObjectUtils.writeValueAsString(
            BlockDataList(blocks)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity(proposalsPayload, headers)
    }

    @Test
    @Ignore
    fun testReceiveBlocks() {
        val entity = getHttpEntity()

        assertEquals(2, blockRepository.count())
        val block = blockRepository.getLastBlock(DEFAULT_SPACE)
        assertEquals(1, block.height)
        blockRepository.deleteById(block.id!!)
        assertEquals(1, blockRepository.count())

        restTemplate.postForObject(url, entity, String::class.java)

        assertEquals(2, blockRepository.count())
        val inserted = blockRepository.getLastBlock(DEFAULT_SPACE)
        assertEquals(block, inserted)
    }

    @Test
    fun testReceiveBlocksTwice() {
        val blocks = eventProcessorService.onProduceBlock()

        assertTrue(blocks.isNotEmpty())

        val doubleBlocks = blocks.plus(blocks)

        val proposalsPayload = ObjectUtils.writeValueAsString(
            BlockDataList(doubleBlocks)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(proposalsPayload, headers)

        restTemplate.postForObject(url, entity, String::class.java)
    }

    @Test
    @Ignore
    fun testReceiveBlocksWrongRound() {

        val entity = getHttpEntity()

        assertEquals(2, blockRepository.count())
        val block = blockRepository.getLastBlock(DEFAULT_SPACE)
        assertEquals(1, block.height)
        blockRepository.deleteById(block.id!!)
        assertEquals(1, blockRepository.count())

        TimeUtils.setTestTime(Application.ROUND + 1L)
        restTemplate.postForObject(url, entity, String::class.java)

        assertEquals(1, blockRepository.count())
    }
}