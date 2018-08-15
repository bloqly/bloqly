package org.bloqly.machine.controller.data

import org.bloqly.machine.controller.exception.NotFoundException
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.BlockRequest
import org.bloqly.machine.vo.BlockVO
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController()
@RequestMapping("/api/v1/data/blocks")
class BlockController(
    private val blockService: BlockService
) {

    @PostMapping("last")
    fun getLastBlock(@RequestBody blockRequest: BlockRequest): BlockVO {
        return blockService.getLastBlockForSpace(blockRequest.spaceId).toVO()
    }

    @PostMapping("lib")
    fun getLIB(@RequestBody blockRequest: BlockRequest): BlockVO {
        return blockService.getLIBForSpace(blockRequest.spaceId).toVO()
    }

    @PostMapping("search")
    fun getDelta(@RequestBody blockRequest: BlockRequest): BlockDataList {
        return blockService.getBlockDataList(blockRequest)
    }

    @PostMapping("{blockHash}")
    fun getBlock(@PathVariable("blockHash") blockHash: String): BlockVO {
        val block = blockService.findByHash(blockHash) ?: throw NotFoundException()
        return block.toVO()
    }
}