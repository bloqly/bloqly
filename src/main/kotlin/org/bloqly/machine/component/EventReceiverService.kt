package org.bloqly.machine.component

import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventReceiverService(
    private val eventProcessorService: EventProcessorService,
    private val accountRepository: AccountRepository
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { vote ->
            try {
                eventProcessorService.onVote(
                    vote.toModel(accountRepository.findValidatorByPublicKey(vote.publicKey))
                )
            } catch (e: Exception) {
                log.error("Could not process vote $vote", e)
            }
        }
    }

    fun receiveProposals(proposals: List<BlockData>) {
        eventProcessorService.onProposals(proposals)
    }
}