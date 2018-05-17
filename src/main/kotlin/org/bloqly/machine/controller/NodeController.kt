package org.bloqly.machine.controller

import org.bloqly.machine.component.SerializationService
import org.bloqly.machine.service.NodeService
import org.bloqly.machine.vo.NodeListVO
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController()
@RequestMapping("/nodes")
class NodeController(
    private val nodeService: NodeService,
    private val serializationService: SerializationService) {

    @GetMapping
    fun getNodes(): NodeListVO {

        val nodes = nodeService.getAllNodes()

        return NodeListVO(
                nodes = nodes.map { serializationService.nodeToVO(it) }
        )
    }

}