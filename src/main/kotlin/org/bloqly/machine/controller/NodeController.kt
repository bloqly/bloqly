package org.bloqly.machine.controller

import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.NodeList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController()
@RequestMapping("/data/nodes")
class NodeController(private val nodeService: NodeService) {

    @GetMapping
    fun getNodes(): NodeList {

        val nodes = nodeService.getAllNodes()

        return NodeList(nodes = nodes.map { it.toVO() })
    }
}