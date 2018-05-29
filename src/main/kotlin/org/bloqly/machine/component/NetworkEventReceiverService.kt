package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("production")
class NetworkEventReceiverService(private val eventProcessorService: EventProcessorService) : EventReceiverService {

    override fun receiveTransactions(transactionVOs: List<TransactionVO>) {

        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    override fun receiveVotes(voteVOs: List<VoteVO>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveProposals(proposals: List<BlockDataVO>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}