package org.bloqly.machine.component

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

    @Scheduled(fixedDelay = 5000)
    fun queryForNodes() {
        nodeQueryService.queryForNodes()
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    fun sendTransactions() {

        val transactions = transactionService.getNewTransactions()

        if (transactions.isNotEmpty()) {

            log.info("Sending ${transactions.size} transactions.")

            eventSenderService.sendTransactions(transactions)
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    fun sendVotes() {
        val votes = eventProcessorService.onGetVotes()

        if (votes.isNotEmpty()) {

            log.info("Sending ${votes.size} votes.")

            eventSenderService.sendVotes(votes)
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 3000)
    fun sendProposals() {

        val proposals = eventProcessorService.onGetProposals()

        if (proposals.isNotEmpty()) {

            log.info("Sending ${proposals.size} proposals.")

            eventSenderService.sendProposals(proposals)
        } else {
            log.warn("No new block proposals to send.")
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 4000)
    fun selectBestProposal() {
        eventProcessorService.onSelectBestProposal()
    }

    //@Scheduled(fixedDelay = 5000)
    fun checkDeltas() {
        val deltas = deltaService.getDeltas()

        deltas.forEach { delta ->
            val blocks = eventSenderService.requestDelta(delta)

            blocks?.forEach { block ->
                eventProcessorService.onProposals(listOf(block))
                eventProcessorService.onSelectBestProposal()
            }
        }
    }
}