package org.bloqly.machine.component

import org.bloqly.machine.service.TransactionService
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(SchedulerService::class.simpleName)

    @Scheduled(fixedDelay = 5000)
    fun queryForNodes() {

        log.info("Query for nodes")

        nodeQueryService.queryForNodes()
    }

    @Scheduled(fixedDelay = 5000)
    fun sendTransactions() {

        log.info("Send transactions")

        val transactions = transactionService.getNewTransactions()

        log.info("Found ${transactions.size} transactions to send")

        if (transactions.isNotEmpty()) {

            val transactionVOs = serializationService.transactionsToVO(transactions)
            eventSenderService.sendTransactions(transactionVOs);
        }
    }

}