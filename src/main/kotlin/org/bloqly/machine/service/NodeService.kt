package org.bloqly.machine.service

import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.repository.NodeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import javax.annotation.PostConstruct

@Service
@Transactional(isolation = SERIALIZABLE)
class NodeService(
    private val nodeRepository: NodeRepository,
    @Value("\${nodes:}") private val nodes: Array<String>,
    @Value("\${server.port}") private val serverPort: Int
) {

    @PostConstruct
    fun init() {
        parseAndAddNodes(nodes)
    }

    private fun parseAndAddNodes(nodes: Array<String>) {

        nodes.map(String::trim)
            .filter { it.isNotEmpty() }
            .map { it.split(":") }.forEach { (host, port) ->

                val nodeId = NodeId(
                    host = host,
                    port = port.toInt()
                )

                if (!nodeRepository.existsById(nodeId)) {

                    nodeRepository.save(
                        Node(
                            id = nodeId,
                            addedTime = Instant.now().toEpochMilli()
                        )
                    )
                }
            }
    }

    @Transactional
    fun addNode(node: Node) {

        if (node.id.port == serverPort) {
            return
        }

        if (nodeRepository.existsById(node.id)) {
            return
        }

        nodeRepository.save(
            Node(
                id = node.id,
                addedTime = Instant.now().toEpochMilli()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getAllNodes(): List<Node> {

        return nodeRepository.findAll().toList()
    }

    @Transactional(readOnly = true)
    fun getNodesToQuery(): List<Node> {

        return nodeRepository.findAll().filter { it.id.port != serverPort }
    }
}