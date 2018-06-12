package org.bloqly.machine.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.BlockDataList
import org.junit.After
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
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @LocalServerPort
    private var port: Int = 0

    @Before
    fun init() {
        testService.createBlockchain()
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testReceiveBlocks() {

        val url = "http://localhost:$port/blocks"

        val blocks = eventProcessorService.onGetProposals()

        val proposalsPayload = objectMapper.writeValueAsString(
            BlockDataList.fromBlocks(blocks)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity<String>(proposalsPayload, headers)

        val block = blocks.first()

        //voteRepository.deleteById(vote.id)

        //Assert.assertFalse(voteRepository.existsById(vote.id))

        restTemplate.postForObject(url, entity, Void.TYPE)

        //Assert.assertTrue(voteRepository.existsById(vote.id))
    }
}