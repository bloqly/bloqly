package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO

interface EventReceiverService {

    fun receiveTransactions(transactionVOs: List<TransactionVO>)

    fun receiveVotes(voteVOs: List<VoteVO>)

    fun receiveProposals(proposals: List<BlockDataVO>)

}