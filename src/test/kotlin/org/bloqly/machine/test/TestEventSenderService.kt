package org.bloqly.machine.test

import org.bloqly.machine.component.EventSenderService
import org.bloqly.machine.component.SerializationService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class TestEventSenderService(

        private val eventReceiver: TestEventReceiverService,

        private val serializationService: SerializationService

) : EventSenderService {

    override fun sendTransactions(transactions: List<TransactionVO>) {

        eventReceiver.receiveTransactions(transactions)
    }

    override fun sendVotes(votes: List<VoteVO>) {

        eventReceiver.receiveVotes(votes)
    }

    override fun sendProposals(proposals: List<BlockDataVO>) {

        eventReceiver.receiveProposals(proposals)
    }
}