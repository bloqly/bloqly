package org.bloqly.machine.controller.data

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.node.NodeList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api(
    value = "/api/v1/data/nodes",
    description = "Operations implementing nodes list communications",
    consumes = "application/json",
    produces = "application/json"
)
@Profile("server")
@RestController()
@RequestMapping("/api/v1/data/nodes")
class NodeController(private val nodeService: NodeService) {

    @ApiOperation(
        value = "Returns list of nodes",
        nickname = "getNodes",
        response = NodeList::class
    )
    @GetMapping
    fun getNodes(): NodeList {

        val nodes = nodeService.getAllNodes()

        return NodeList(nodes = nodes.map { it.toVO() })
    }
}