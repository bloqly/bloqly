package org.bloqly.machine.component

import org.bloqly.machine.model.Node
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.NodeListVO
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class NodeQueryService(
    private val nodeService: NodeService,
    private val restTemplate: RestTemplate
) {

    private val log = LoggerFactory.getLogger(NodeQueryService::class.simpleName)

    fun queryForNodes() {

        val nodes = nodeService.getNodesToQuery()

        log.info("Found ${nodes.size} hosts, start querying for nodes list")

        nodes.forEach { queryForNodes(it) }
    }

    private fun queryForNodes(node: Node) {

        val server = node.getServer()
        val path = "http://$server/nodes"

        log.info("Query host $server for nodes")

        val nodeList = try {
            restTemplate.getForObject(path, NodeListVO::class.java)
        } catch (e: Exception) {
            log.info("Could not query path $path for nodes. ${e.message}")
            NodeListVO(nodes = emptyList())
        }

        if (nodeList.nodes.isNotEmpty()) {
            log.info("Node $server returned  ${nodeList.nodes.size} nodes")

            nodeList.nodes.forEach { nodeService.addNode(it.toModel()) }
        }
    }

    fun sendTransactions(node: Node, transactions: List<Transaction>) {

        val server = node.getServer()
        val path = "http://$server/transactions"

        try {
            val entity = HttpEntity(TransactionList.fromTransactions(transactions))
            val res = restTemplate.postForEntity(path, entity, Void.TYPE)

            require(res.statusCode != HttpStatus.OK) {
                "Expected status OK, received ${res.statusCode}"
            }
        } catch (e: Exception) {
            log.error("Could not send transactions to $server. ${e.message}")
        }
    }

    fun sendVotes(node: Node, votes: List<Vote>) {

        val server = node.getServer()
        val path = "http://$server/votes"

        try {

            val entity = HttpEntity(VoteList.fromVotes(votes))

            val res = restTemplate.postForEntity(path, entity, Void.TYPE)

            require(res.statusCode != HttpStatus.OK) {
                "Expected status OK, received ${res.statusCode}"
            }
        } catch (e: Exception) {
            log.error("Could not send votes to $server. ${e.message}")
        }
    }
}