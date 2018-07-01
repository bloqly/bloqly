package org.bloqly.machine.controller

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockDataList
import org.junit.After
import org.junit.Assert.assertEquals
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
class BlockControllerTest {

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var blockRepository: BlockRepository

    @Autowired
    private lateinit var blockCandidateRepository: BlockCandidateRepository

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

    private lateinit var blockCandidateId: BlockCandidateId

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        TimeUtils.setTestTime(0)

        node = Node(NodeId("localhost", port), 0)

        eventReceiverService.receiveVotes(testService.getVotes())

        url = APIUtils.getDataPath(node, "blocks")

        val validator = accountService.getActiveProducerBySpace(
            testService.getDefaultSpace(), TimeUtils.getCurrentRound()
        )

        blockCandidateId = BlockCandidateId(
            spaceId = DEFAULT_SPACE,
            height = 1,
            round = TimeUtils.getCurrentRound(),
            proposerId = validator.id
        )
    }

    @After
    fun tearDown() {
        TimeUtils.reset()
    }

    private fun getHttpEntity(): HttpEntity<String> {
        val blocks = eventProcessorService.onGetProposals()

        assertTrue(blocks.isNotEmpty())

        val proposalsPayload = ObjectUtils.writeValueAsString(
            BlockDataList(blocks)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity(proposalsPayload, headers)
    }

    @Test
    fun testReceiveBlocks() {

        val entity = getHttpEntity()

        assertEquals(1, blockRepository.count())
        blockCandidateRepository.deleteById(blockCandidateId)

        assertFalse(blockCandidateRepository.existsById(blockCandidateId))

        restTemplate.postForObject(url, entity, String::class.java)

        assertEquals(1, blockRepository.count())

        assertTrue(blockCandidateRepository.existsById(blockCandidateId))
    }

    @Test
    fun testReceiveBlocksTwice() {
        val blocks = eventProcessorService.onGetProposals()

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
    fun testReceiveBlocksWrongRound() {

        val entity = getHttpEntity()

        blockCandidateRepository.deleteById(blockCandidateId)
        assertFalse(blockCandidateRepository.existsById(blockCandidateId))

        TimeUtils.setTestTime(Application.ROUND + 1L)
        restTemplate.postForObject(url, entity, String::class.java)

        assertFalse(blockCandidateRepository.existsById(blockCandidateId))
    }
}