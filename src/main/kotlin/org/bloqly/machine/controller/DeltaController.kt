package org.bloqly.machine.controller

import org.bloqly.machine.service.BlockService
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.Delta
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController()
@RequestMapping("/deltas")
class DeltaController(
    private val blockService: BlockService
) {

    @PostMapping
    fun getDelta(@RequestBody delta: Delta) : BlockDataList{

        return blockService.getBlockDataList(delta)
    }
}