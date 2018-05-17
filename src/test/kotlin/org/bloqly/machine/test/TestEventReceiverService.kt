package org.bloqly.machine.test

import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.component.SerializationService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class TestEventReceiverService(

        private val eventProcessorService: EventProcessorService,

        private val serializationService: SerializationService

) : EventReceiverService {

    override fun receiveTransactions(transactionVOs: List<TransactionVO>) {

        transactionVOs.forEach { transactionVO ->

            val transaction = serializationService.transactionFromVO(transactionVO)

            eventProcessorService.onTransaction(transaction)
        }

    }

    override fun receiveVotes(voteVOs: List<VoteVO>) {

        voteVOs.forEach { voteVO ->

            val vote = serializationService.voteFromVO(voteVO)

            eventProcessorService.onVote(vote)

        }

    }

    override fun receiveProposals(proposals: List<BlockDataVO>) {

        eventProcessorService.onProposals(
                proposals.map { serializationService.blockDataFromVO(it) }
        )
    }
}