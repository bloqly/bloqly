package org.bloqly.machine.component

import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.TransactionRequest
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventReceiverService(
    private val eventProcessorService: EventProcessorService,
    private val accountService: AccountService,
    private val transactionService: TransactionService
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveTransactionRequest(transactionRequest: TransactionRequest) {
        val tx = transactionService.createTransaction(transactionRequest)
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { vote ->
            try {
                eventProcessorService.onVote(
                    vote.toModel(accountService.getAccountByPublicKey(vote.publicKey))
                )
            } catch (e: Exception) {
                val errorMessage = "Could not process vote $vote"
                log.warn(errorMessage)
                log.error(errorMessage, e)
            }
        }
    }

    fun receiveProposals(proposals: List<BlockData>) {
        eventProcessorService.onProposals(proposals)
    }
}