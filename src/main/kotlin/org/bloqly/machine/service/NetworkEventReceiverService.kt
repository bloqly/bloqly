package org.bloqly.machine.service

import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("production")
class NetworkEventReceiverService : EventReceiverService {

    override fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveVotes(voteVOs: List<VoteVO>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveProposals(proposals: List<BlockDataVO>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}