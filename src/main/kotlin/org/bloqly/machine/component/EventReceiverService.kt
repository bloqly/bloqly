package org.bloqly.machine.component

import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventReceiverService(
    private val eventProcessorService: EventProcessorService
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { voteVO ->
            try {
                eventProcessorService.onVote(voteVO.toModel())
            } catch (e: Exception) {
                log.error("Could not process vote $voteVO", e)
            }
        }
    }

    fun receiveProposals(proposals: List<BlockData>) {
        eventProcessorService.onProposals(proposals)
    }
}