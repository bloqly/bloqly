package org.bloqly.machine.controller

import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.BlockDataList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/blocks")
class BlockController(
    private val eventReceiverService: EventReceiverService
) {

    @PostMapping
    fun onBlock(@RequestBody blockDataList: BlockDataList) {

        eventReceiverService.receiveProposals(blockDataList.blocks)
    }
}