package org.bloqly.machine.test

import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class TestEventReceiverService(private val eventProcessorService: EventProcessorService) : EventReceiverService {

    override fun receiveTransactions(transactionVOs: List<TransactionVO>) {

        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    override fun receiveVotes(voteVOs: List<VoteVO>) {

        voteVOs.forEach { eventProcessorService.onVote(it.toModel()) }
    }

    override fun receiveProposals(proposals: List<BlockDataVO>) {

        eventProcessorService.onProposals(proposals.map { it.toModel() })
    }
}