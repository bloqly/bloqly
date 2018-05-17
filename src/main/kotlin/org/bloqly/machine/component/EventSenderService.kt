package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO

interface EventSenderService {

    fun sendVotes(votes: List<VoteVO>)

    fun sendTransactions(transactions: List<TransactionVO>)

    fun sendProposals(proposals: List<BlockDataVO>)

}