package org.bloqly.machine.component

import org.bloqly.machine.service.DeltaService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("scheduler")

// TODO change to cron expressions?

class SchedulerService(
    private val nodeQueryService: NodeQueryService,
    private val eventSenderService: EventSenderService,
    private val eventProcessorService: EventProcessorService,
    private val blockProcessor: BlockProcessor,
    private val deltaService: DeltaService
) {

    private val log = LoggerFactory.getLogger(SchedulerService::class.simpleName)

    //@Scheduled(fixedDelay = 30000)
    fun queryForNodes() {
        nodeQueryService.queryForNodes()
    }

    //@Scheduled(initialDelay = 500, fixedDelay = 1000)
    fun sendTransactions() {

        val transactions = blockProcessor.getPendingTransactions()

        if (transactions.isNotEmpty()) {

            log.info("Transactions to send: ${transactions.size}.")

            eventSenderService.sendTransactions(transactions)
        }
    }

    //@Scheduled(initialDelay = 1000, fixedDelay = 1000)
    fun sendVotes() {
        eventProcessorService.onGetVotes().takeIf { it.isNotEmpty() }?.let { votes ->
            log.info("Votes to send: ${votes.size}.")

            eventSenderService.sendVotes(votes)
        }
    }

    //@Scheduled(initialDelay = 1500, fixedDelay = 1000)
    fun sendProposals() {

        val proposals = eventProcessorService.onProduceBlock()

        if (proposals.isNotEmpty()) {

            log.info("Block proposals to send: ${proposals.size}.")

            eventSenderService.sendProposals(proposals)
        }
    }

    //@Scheduled(fixedDelay = 5000)
    fun checkDeltas() {
        val deltas = deltaService.getDeltas()

        if (deltas.isNotEmpty()) {
            eventSenderService.requestDeltas(deltas)
        }
    }
}