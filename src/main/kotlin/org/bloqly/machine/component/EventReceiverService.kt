package org.bloqly.machine.component

import org.bloqly.machine.model.VoteType
import org.bloqly.machine.repository.PropertyRepository
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
    private val propertyRepository: PropertyRepository
) {
    private val log = LoggerFactory.getLogger(EventReceiverService::class.simpleName)

    fun receiveTransactions(transactionVOs: List<TransactionVO>) {
        transactionVOs.forEach { eventProcessorService.onTransaction(it.toModel()) }
    }

    fun receiveVotes(voteVOs: List<VoteVO>) {
        voteVOs.forEach { voteVO ->
            try {
                val vote = voteVO.toModel()
                eventProcessorService.onVote(vote)
            } catch (e: Exception) {
                log.error("Could not process vote $voteVO", e)
            }
        }
    }

    fun receiveProposals(proposals: List<BlockData>) {
        val validatedProposals = proposals
            .filter { proposal ->
                val quorum = propertyRepository.getQuorumBySpaceId(proposal.block.spaceId)
                proposal.votes.count { it.voteType == VoteType.VOTE.name } >= quorum
            }

        eventProcessorService.onProposals(validatedProposals)
    }
}