package org.bloqly.machine.component

import org.bloqly.machine.model.BlockData
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.NodeList
import org.bloqly.machine.vo.TransactionList
import org.bloqly.machine.vo.VoteList
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
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
            restTemplate.getForObject(path, NodeList::class.java)
        } catch (e: Exception) {
            log.info("Could not query path $path for nodes. ${e.message}")
            NodeList(nodes = emptyList())
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

            checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
        } catch (e: Exception) {
            log.error("Could not send transactions to $server. Details: ${e.message}")
        }
    }

    fun sendVotes(node: Node, votes: List<Vote>) {

        val server = node.getServer()
        val path = "http://$server/votes"

        try {

            val entity = HttpEntity(VoteList.fromVotes(votes))

            checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
        } catch (e: Exception) {
            log.error("Could not send votes to $server. Details: ${e.message}")
        }
    }

    fun sendProposals(node: Node, proposals: List<BlockData>) {

        val server = node.getServer()
        val path = "http://$server/blocks"

        try {

            val entity = HttpEntity(BlockDataList.fromBlocks(proposals))

            checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
        } catch (e: Exception) {
            log.error("Could not send proposals to $server. Details: ${e.message}")
        }
    }

    private fun checkResponse(response: ResponseEntity<String>) {
        require(response.statusCode.is2xxSuccessful) {
            "Received unexpected response $response."
        }
    }
}