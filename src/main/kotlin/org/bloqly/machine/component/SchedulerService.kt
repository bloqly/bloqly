package org.bloqly.machine.component

import org.bloqly.machine.service.TransactionService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@Profile("server")
class SchedulerService(

    private val nodeQueryService: NodeQueryService,
    private val transactionService: TransactionService,
    private val eventSenderService: EventSenderService,
    private val serializationService: SerializationService

) {

    @Scheduled(fixedDelay = 5000)
    fun queryForNodes() {

        nodeQueryService.queryForNodes()
    }

    @Scheduled(fixedDelay = 5000)
    fun sendTransactions() {

        val transactions = transactionService.getNewTransactions()

        val transactionVOs = serializationService.transactionsToVO(transactions)

        eventSenderService.sendTransactions(transactionVOs);
    }

}