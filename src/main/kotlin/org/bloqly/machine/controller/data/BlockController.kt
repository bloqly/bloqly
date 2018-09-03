package org.bloqly.machine.controller.data

import org.bloqly.machine.service.BlockService
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.block.BlockRequest
import org.bloqly.machine.vo.block.BlockVO
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
    fun getLastBlock(@RequestBody blockRequest: BlockRequest): ResponseEntity<BlockVO> {
        return if (blockService.existsBySpaceId(blockRequest.spaceId)) {
            ResponseEntity(blockService.getLastBlockBySpace(blockRequest.spaceId).toVO(), OK)
        } else {
            ResponseEntity(NOT_FOUND)
        }
    }

    @PostMapping("lib")
    fun getLIB(@RequestBody blockRequest: BlockRequest): ResponseEntity<BlockVO> {
        return if (blockService.existsBySpaceId(blockRequest.spaceId)) {
            val lastBlock = blockService.getLastBlockBySpace(blockRequest.spaceId)
            ResponseEntity(blockService.getLIBForBlock(lastBlock).toVO(), OK)
        } else {
            ResponseEntity(NOT_FOUND)
        }
    }

    @PostMapping("search")
    fun getDelta(@RequestBody blockRequest: BlockRequest): ResponseEntity<BlockDataList> {
        return ResponseEntity(blockService.getBlockDataList(blockRequest), OK)
    }

    @GetMapping("{blockHash}")
    fun getBlock(@PathVariable("blockHash") blockHash: String): ResponseEntity<BlockVO> {
        return blockService.findByHash(blockHash)
            ?.let { ResponseEntity(it.toVO(), OK) }
            ?: ResponseEntity(NOT_FOUND)
    }
}