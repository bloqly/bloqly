package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.test.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
open class EventReceiverServiceTest : BaseTest() {

    @Test
    fun testAccountPublicKeyPopulatedWhenReceiveVotes() {

        val votes = eventProcessorService.onGetVotes()

        val votesVOs = votes.map { it.toVO() }

        voteRepository.deleteAll()
        accountRepository.deleteAll()

        eventReceiverService.receiveVotes(votesVOs)

        assertEquals(4, accountRepository.count())
    }
}