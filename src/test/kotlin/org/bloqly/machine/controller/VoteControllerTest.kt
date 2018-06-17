package org.bloqly.machine.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.VoteList
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
class VoteControllerTest {

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var voteRepository: VoteRepository

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
    fun testReceiveVotes() {

        val url = "http://localhost:$port/data/votes"

        val votes = eventProcessorService.onGetVotes()

        val votesPayload = objectMapper.writeValueAsString(
            VoteList.fromVotes(votes)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity<String>(votesPayload, headers)

        val vote = votes.first()

        voteRepository.deleteById(vote.id)

        assertFalse(voteRepository.existsById(vote.id))

        restTemplate.postForObject(url, entity, Void.TYPE)

        assertTrue(voteRepository.existsById(vote.id))
    }
}