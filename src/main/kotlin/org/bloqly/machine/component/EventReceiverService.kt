package org.bloqly.machine.component

import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventReceiverService(
    private val eventProcessorService: EventProcessorService,
    private val propertyRepository: PropertyRepository
) {

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { eventProcessorService.onVote(it.toModel()) }
    }

    fun receiveProposals(proposals: List<BlockDataVO>) {
        val validatedProposals = proposals
            .filter { proposal ->
                val quorum = propertyRepository.getQuorum(proposal.block.space)
                proposal.votes.size >= quorum
            }
            .map { it.toModel() }

        eventProcessorService.onProposals(validatedProposals)
    }
}