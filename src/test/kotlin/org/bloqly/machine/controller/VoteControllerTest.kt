package org.bloqly.machine.controller

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.VoteList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var eventProcessorService: EventProcessorService

    @LocalServerPort
    private var port: Int = 0

    private lateinit var node: Node

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()

        node = Node(NodeId("localhost", port), 0)
    }

    @Test
    fun testReceiveVotes() {

        val url = APIUtils.getDataPath(node, "votes")

        val votes = eventProcessorService.onGetVotes()

        val votesPayload = ObjectUtils.writeValueAsString(
            VoteList.fromVotes(votes)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(votesPayload, headers)

        val vote = votes.first()

        voteRepository.deleteById(vote.id!!)

        assertFalse(voteRepository.existsById(vote.id!!))

        restTemplate.postForObject(url, entity, Void.TYPE)

        assertNotNull(voteRepository.findBySpaceIdAndValidatorAndHeight(DEFAULT_SPACE, vote.validator, vote.height))
    }
}