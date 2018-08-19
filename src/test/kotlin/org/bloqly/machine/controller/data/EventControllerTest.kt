package org.bloqly.machine.controller.data

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.test.BaseControllerTest
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class EventControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var blockRepository: BlockRepository

    @Autowired
    private lateinit var eventReceiverService: EventReceiverService

    @Before
    override fun setup() {
        super.setup()

        eventReceiverService.receiveVotes(testService.getVotes())
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
    // TODO fix
    fun testReceiveBlocks() {
        val entity = getHttpEntity()

        assertEquals(2, blockRepository.count())
        val block = blockRepository.getLastBlock(DEFAULT_SPACE)
        assertEquals(1, block.height)
        blockRepository.deleteById(block.id!!)
        assertEquals(1, blockRepository.count())

        val url = APIUtils.getEventPath(node, "blocks")

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

        val url = APIUtils.getEventPath(node, "blocks")

        restTemplate.postForObject(url, entity, String::class.java)
    }

    @Test
    @Ignore
    // TODO fix
    fun testReceiveBlocksWrongRound() {

        val entity = getHttpEntity()

        assertEquals(2, blockRepository.count())
        val block = blockRepository.getLastBlock(DEFAULT_SPACE)
        assertEquals(1, block.height)
        blockRepository.deleteById(block.id!!)
        assertEquals(1, blockRepository.count())

        TimeUtils.setTestTime(Application.ROUND + 1L)

        val url = APIUtils.getEventPath(node, "blocks")
        restTemplate.postForObject(url, entity, String::class.java)

        assertEquals(1, blockRepository.count())
    }

    @Test
    fun testReceiveVotes() {

        val votes = eventProcessorService.onGetVotes()

        val votesPayload = ObjectUtils.writeValueAsString(
            VoteList.fromVotes(votes)
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(votesPayload, headers)

        val vote = votes.first()

        voteRepository.deleteById(vote.id!!)

        Assert.assertFalse(voteRepository.existsById(vote.id!!))

        val url = APIUtils.getEventPath(node, "votes")

        restTemplate.postForObject(url, entity, String::class.java)

        Assert.assertNotNull(
            voteRepository.findByValidatorAndBlockHash(
                vote.validator,
                vote.blockHash
            )
        )
    }

    @Test
    fun testReceiveTransactions() {

        val transaction = testService.createTransaction()

        val transactionPayload = ObjectUtils.writeValueAsString(
            TransactionList.fromTransactions(listOf(transaction))
        )

        val entity = HttpEntity(transactionPayload, headers)

        val url = APIUtils.getEventPath(node, "transactions")

        restTemplate.postForObject(url, entity, String::class.java)

        assertTrue(transactionRepository.existsByHash(transaction.hash))
    }
}