package org.bloqly.machine.component

import org.bloqly.machine.model.Node
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.util.APIUtils
import org.bloqly.machine.vo.block.BlockData
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.block.BlockRangeRequest
import org.bloqly.machine.vo.node.NodeList
import org.bloqly.machine.vo.transaction.TransactionList
import org.bloqly.machine.vo.vote.VoteList
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

        val path = APIUtils.getDataPath(node.id.toString(), "nodes")

        log.debug("Query host $node for nodes")

        val nodeList = try {
            restTemplate.getForObject(path, NodeList::class.java)
        } catch (e: Exception) {
            log.warn("Could not query path $path for nodes. ${e.message}")
            NodeList(nodes = emptyList())
        }

        if (nodeList.nodes.isNotEmpty()) {
            log.info("Node $node returned  ${nodeList.nodes.size} nodes")

            nodeList.nodes.forEach { nodeService.addNode(it.toModel()) }
        }
    }

    fun sendTransactions(node: Node, transactions: List<Transaction>) {

        val path = APIUtils.getEventPath(node.id.toString(), "transactions")

        val entity = HttpEntity(TransactionList.fromTransactions(transactions))

        checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
    }

    fun sendVotes(node: Node, votes: List<Vote>) {

        val path = APIUtils.getEventPath(node.id.toString(), "votes")

        val entity = HttpEntity(VoteList.fromVotes(votes))

        checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
    }

    fun sendProposals(node: Node, proposals: List<BlockData>) {

        val path = APIUtils.getEventPath(node.id.toString(), "blocks")

        val entity = HttpEntity(BlockDataList(proposals))

        checkResponse(restTemplate.postForEntity(path, entity, String::class.java))
    }

    private fun checkResponse(response: ResponseEntity<String>) {
        require(response.statusCode.is2xxSuccessful) {
            "Received unexpected response $response."
        }
    }

    // TODO create test for deltas API
    fun requestDelta(node: Node, blockRangeRequest: BlockRangeRequest): List<BlockData>? {
        val path = APIUtils.getDataPath(node.id.toString(), "blocks/search")

        var result: List<BlockData>? = null

        try {

            val blockDataList = restTemplate.postForObject(path, blockRangeRequest, BlockDataList::class.java)

            result = blockDataList?.blocks
        } catch (e: Exception) {
            val errorMessage = "Could not retrieve deltas from $node: ${e.message}"
            log.warn(errorMessage)
            log.error(errorMessage, e)
        }

        return result
    }
}