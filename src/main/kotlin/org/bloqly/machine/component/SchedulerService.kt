package org.bloqly.machine.component

import org.bloqly.machine.model.Node
import org.bloqly.machine.service.NodeService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
@Profile("server")
class SchedulerService(

    private val nodeQueryService: NodeQueryService,
    private val nodeService: NodeService) {

    private val log = LoggerFactory.getLogger(SchedulerService::class.simpleName)

    @Scheduled(fixedDelay = 5000)
    fun queryForNodes() {

        val nodes = getNodes()

        log.debug("Found ${nodes.size} hosts, start querying for nodes list")

        nodes.forEach {
            nodeQueryService.queryForNodes(it)
        }
    }

    @Scheduled(fixedDelay = 5000)
    fun queryForTransactions() {

        val nodes = getNodes()

        log.debug("Found ${nodes.size} hosts, start querying for transactions")

        nodes.forEach { nodeQueryService.queryForTransactions(it) }
    }

    private fun getNodes(): List<Node> {
        return nodeService.getNodesToQuery()
    }
}