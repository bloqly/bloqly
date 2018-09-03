package org.bloqly.machine.controller.data

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.transaction.TransactionList
import org.bloqly.machine.vo.vote.VoteList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api(
    value = "/api/v1/data/event",
    description = "Operations implementing event processing",
    consumes = "application/json",
    produces = "application/json"
)
@Profile("server")
@RestController
@RequestMapping("/api/v1/data/event")
class EventController(private val eventReceiverService: EventReceiverService) {

    @ApiOperation(
        value = "Handles new blocks event",
        nickname = "onBlocks"
    )
    @PostMapping("/blocks")
    fun onBlocks(@RequestBody blockDataList: BlockDataList) {
        eventReceiverService.onBlocks(blockDataList.blocks)
    }

    @ApiOperation(
        value = "Handles new transactions event",
        nickname = "onTransactions"
    )
    @PostMapping("/transactions")
    fun onTransactions(@RequestBody transactionsList: TransactionList) {
        eventReceiverService.receiveTransactions(transactionsList.transactions)
    }

    @ApiOperation(
        value = "Handles new votes event",
        nickname = "onVotes"
    )
    @PostMapping("/votes")
    fun onVotes(@RequestBody votesList: VoteList) {
        eventReceiverService.receiveVotes(votesList.votes)
    }
}