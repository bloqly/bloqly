package org.bloqly.machine.controller

import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.component.SerializationService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.vo.TransactionListVO
import org.bloqly.machine.vo.TransactionVO
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val eventReceiverService: EventReceiverService,
    private val transactionService: TransactionService,
    private val serializationService: SerializationService) {

    @PostMapping
    fun onTransaction(@RequestBody transactionVO: TransactionVO) {

        eventReceiverService.receiveTransactions(listOf(transactionVO))
    }

    @GetMapping
    fun getTransactions(): TransactionListVO {

        val transactions = transactionService.getNewTransactions()

        // TODO: move to serialization service
        return TransactionListVO(
                transactions = transactions.map { serializationService.transactionToVO(it) }
        )
    }

}