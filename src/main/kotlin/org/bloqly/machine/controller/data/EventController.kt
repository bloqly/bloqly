package org.bloqly.machine.controller.data

import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/data/event")
class EventController(
    private val eventReceiverService: EventReceiverService
) {

    @PostMapping("/blocks")
    fun onReceiveBlocks(@RequestBody blockDataList: BlockDataList) {
        //eventReceiverService.onBlocks(blockDataList.blocks)
    }

    @PostMapping("/transactions")
    fun onReceiveTransactions(@RequestBody transactionsList: TransactionList) {
        eventReceiverService.receiveTransactions(transactionsList.transactions)
    }

    @PostMapping("/votes")
    fun onReceiveVotes(@RequestBody votesList: VoteList) {
        eventReceiverService.receiveVotes(votesList.votes)
    }
}