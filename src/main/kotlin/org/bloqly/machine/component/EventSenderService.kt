package org.bloqly.machine.component

import org.bloqly.machine.model.EntityEvent
import org.bloqly.machine.model.EntityEventId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.EntityEventRepository
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.Delta
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

    fun sendVotes(votes: List<Vote>) {
        val nodes = nodeService.getNodesToQuery()

        nodes.forEach { node ->
            try {
                log.info("Sending votes to node $node")

                val votesToSend = votes.filter { vote ->
                    !entityEventRepository.existsById(EntityEventId(vote.id.toString(), node.id.toString()))
                }

                if (votesToSend.isNotEmpty()) {
                    nodeQueryService.sendVotes(node, votesToSend)
                }

                votesToSend.forEach { vote ->
                    entityEventRepository.save(
                        EntityEvent(EntityEventId(vote.id.toString(), node.id.toString()), Instant.now().toEpochMilli())
                    )
                }
            } catch (e: Exception) {
                log.error("Could not send votes to $node. Details: ${e.message}")
            }
        }
    }

    fun sendTransactions(transactions: List<Transaction>) {
        val nodes = nodeService.getNodesToQuery()

        nodes.forEach { node ->
            try {
                log.info("Sending transactions to node $node")

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
            } catch (e: Exception) {
                log.error("Could not send transactions to $node. Details: ${e.message}")
            }
        }
    }

    fun sendProposals(proposals: List<BlockData>) {
        val nodes = nodeService.getNodesToQuery()

        nodes.forEach { node ->
            try {
                log.info("Sending proposals to node $node")

                val proposalsToSend = proposals.filter { proposal ->
                    !entityEventRepository.existsById(EntityEventId(proposal.block.id, node.id.toString()))
                }

                if (proposalsToSend.isNotEmpty()) {
                    nodeQueryService.sendProposals(node, proposalsToSend)
                }

                proposalsToSend.forEach { proposal ->
                    entityEventRepository.save(
                        EntityEvent(EntityEventId(proposal.block.id, node.id.toString()), Instant.now().toEpochMilli())
                    )
                }
            } catch (e: Exception) {
                "Could not send proposals to $node. Details: ${e.message}"
            }
        }
    }

    fun requestDelta(delta: Delta): List<BlockData>? {
        val nodes = nodeService.getNodesToQuery()

        val node = nodes.shuffled().first()

        log.info("Request deltas from node $node")

        val deltas = nodeQueryService.requestDelta(node, delta)

        deltas?.forEach { blockData ->
            entityEventRepository.save(
                EntityEvent(EntityEventId(blockData.block.id, node.id.toString()), Instant.now().toEpochMilli())
            )
        }

        return deltas
    }
}