package org.bloqly.machine.controller.data

import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.TransactionRequest
import org.bloqly.machine.vo.TransactionVO
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Profile("server")
@RestController
@RequestMapping("/api/v1/data")
class TransactionController(
    private val eventReceiverService: EventReceiverService,
    private val transactionService: TransactionService
) {

    @PostMapping("/transactions")
    fun onCreateTransaction(@RequestBody transactionRequest: TransactionRequest): TransactionVO {
        return eventReceiverService.receiveTransactionRequest(transactionRequest)
    }

    @PostMapping("/transactions/search")
    fun searchPendingTransactions(request: HttpServletRequest): TransactionList {
        val transactions = transactionService.getRecentTransactions(MAX_REFERENCED_BLOCK_DEPTH)

        return TransactionList.fromTransactions(transactions)
    }
}