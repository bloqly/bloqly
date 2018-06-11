package org.bloqly.machine.component

import org.bloqly.machine.model.EntityEvent
import org.bloqly.machine.model.EntityEventId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.repository.EntityEventRepository
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.VoteVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EventSenderService(
    private val nodeService: NodeService,
    private val nodeQueryService: NodeQueryService,
    private val entityEventRepository: EntityEventRepository
) {

    private val log = LoggerFactory.getLogger(EventSenderService::class.simpleName)

    fun sendVotes(votes: List<VoteVO>) {
        TODO("not implemented")
    }

    fun sendTransactions(transactions: List<Transaction>) {
        val nodes = nodeService.getNodesToQuery()

        nodes.forEach { node ->

            log.info("Send transactions to node $node")

            val txs = transactions.filter { tx ->
                !entityEventRepository.existsById(EntityEventId(tx.id, node.id.toString()))
            }

            if (txs.isNotEmpty()) {
                nodeQueryService.sendTransactions(node, txs)
            }

            txs.forEach { tx ->
                entityEventRepository.save(
                    EntityEvent(EntityEventId(tx.id, node.id.toString()), Instant.now().toEpochMilli())
                )
            }
        }
    }

    fun sendProposals(proposals: List<BlockDataVO>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}