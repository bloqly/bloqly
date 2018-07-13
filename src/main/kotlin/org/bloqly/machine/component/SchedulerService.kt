package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.service.DeltaService
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
    private val eventSenderService: EventSenderService,
    private val eventProcessorService: EventProcessorService,
    private val deltaService: DeltaService
) {

    private val log = LoggerFactory.getLogger(SchedulerService::class.simpleName)

    @Scheduled(fixedDelay = 1000)
    fun queryForNodes() {
        nodeQueryService.queryForNodes()
    }

    @Scheduled(fixedDelay = 1000)
    fun sendTransactions() {

        val transactions = transactionService.getRecentTransactions(MAX_REFERENCED_BLOCK_DEPTH)

        if (transactions.isNotEmpty()) {

            log.info("Transactions to send: ${transactions.size}.")

            eventSenderService.sendTransactions(transactions)
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun sendVotes() {
        val votes = eventProcessorService.onGetVotes()

        if (votes.isNotEmpty()) {

            log.info("Votes to send: ${votes.size}.")

            eventSenderService.sendVotes(votes)
        }
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 500)
    fun sendProposals() {

        val proposals = eventProcessorService.onProduceBlock()

        if (proposals.isNotEmpty()) {

            log.info("Block proposals to send: ${proposals.size}.")

            eventSenderService.sendProposals(proposals)
        }
    }
    
    @Scheduled(fixedDelay = 5000)
    fun checkDeltas() {
        val deltas = deltaService.getDeltas()

        if (deltas.isNotEmpty()) {
            eventSenderService.requestDeltas(deltas)
        }
    }
}