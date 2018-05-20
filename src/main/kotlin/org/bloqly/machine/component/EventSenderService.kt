package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionListVO
import org.bloqly.machine.vo.VoteVO

interface EventSenderService {

    fun sendVotes(votes: List<VoteVO>)

    fun sendTransactions(transactionListVO: TransactionListVO)

    fun sendProposals(proposals: List<BlockDataVO>)

}