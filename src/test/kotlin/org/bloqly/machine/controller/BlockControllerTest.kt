package org.bloqly.machine.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.BlockDataList
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
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @LocalServerPort
    private var port: Int = 0

    @Before
    fun init() {
        testService.cleanup()

        testService.createBlockchain()
    }

    @Test
    fun testReceiveBlocks() {

        val url = "http://localhost:$port/data/blocks"

        val blockDatas = eventProcessorService.onGetProposals()

        val proposalsPayload = objectMapper.writeValueAsString(
            BlockDataList(blockDatas)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity<String>(proposalsPayload, headers)

        val blockData = blockDatas.first()

        assertFalse(blockRepository.existsById(blockData.block.id))

        restTemplate.postForObject(url, entity, Void.TYPE)

        assertFalse(blockRepository.existsById(blockData.block.id))

        val validator = accountService.getActiveValidator(DEFAULT_SPACE, 1)

        assertTrue(
            blockCandidateRepository.existsById(
                BlockCandidateId(
                    space = DEFAULT_SPACE,
                    height = 1,
                    proposerId = validator.id
                )
            )
        )
    }
}