package org.bloqly.machine.component

import org.bloqly.machine.service.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("scheduler")
class SchedulerService(

    private val nodeQueryService: NodeQueryService,
    private val transactionService: TransactionService,
    private val eventSenderService: EventSenderService
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

        val transactions = transactionService.getNewTransactions().map { it.toVO() }

        if (transactions.isNotEmpty()) {

            log.info("Found ${transactions.size} transactions to send")

            eventSenderService.sendTransactions(transactions)
        } else {

            log.info("There are no transactions to send, skipping")
        }
    }
}