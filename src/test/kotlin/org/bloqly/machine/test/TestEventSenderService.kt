package org.bloqly.machine.test

import org.bloqly.machine.component.EventSenderService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionListVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class TestEventSenderService(private val eventReceiver: TestEventReceiverService) : EventSenderService {

    override fun sendTransactions(transactionListVO: TransactionListVO) {

        eventReceiver.receiveTransactions(transactionListVO.transactions)
    }

    override fun sendVotes(votes: List<VoteVO>) {

        eventReceiver.receiveVotes(votes)
    }

    override fun sendProposals(proposals: List<BlockDataVO>) {

        eventReceiver.receiveProposals(proposals)
    }
}