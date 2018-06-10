package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.stereotype.Service

@Service
class EventReceiverService(private val eventProcessorService: EventProcessorService) {

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { eventProcessorService.onVote(it.toModel()) }
    }

    fun receiveProposals(proposals: List<BlockDataVO>) {
        eventProcessorService.onProposals(proposals.map { it.toModel() })
    }
}