package org.bloqly.machine.controller.data

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.block.BlockRangeRequest
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

@Api(
    value = "/api/v1/data/blocks",
    description = "Operations providing data access to blocks",
    consumes = "application/json",
    produces = "application/json"
)
@Profile("server")
@RestController()
@RequestMapping("/api/v1/data/blocks")
class BlockController(
    private val blockService: BlockService
) {

    @ApiOperation(
        value = "Returns last best known block",
        response = BlockVO::class,
        nickname = "getLastBlock"
    )
    @PostMapping("last")
    fun getLastBlock(@RequestBody blockRequest: BlockRequest): ResponseEntity<BlockVO> {
        return if (blockService.existsBySpaceId(blockRequest.spaceId)) {
            ResponseEntity(blockService.getLastBlockBySpace(blockRequest.spaceId).toVO(), OK)
        } else {
            ResponseEntity(NOT_FOUND)
        }
    }

    @ApiOperation(
        value = "Returns LIB (last irreversible block)",
        response = BlockVO::class,
        nickname = "getLIB"
    )
    @PostMapping("lib")
    fun getLIB(@RequestBody blockRequest: BlockRequest): ResponseEntity<BlockVO> {
        return if (blockService.existsBySpaceId(blockRequest.spaceId)) {
            val lastBlock = blockService.getLastBlockBySpace(blockRequest.spaceId)
            ResponseEntity(blockService.getLIBForBlock(lastBlock).toVO(), OK)
        } else {
            ResponseEntity(NOT_FOUND)
        }
    }

    @ApiOperation(
        value = "Returns blocks range",
        response = BlockDataList::class,
        nickname = "getBlocksRange"
    )
    @PostMapping("search")
    fun getDelta(@RequestBody blockRangeRequest: BlockRangeRequest): ResponseEntity<BlockDataList> {
        return ResponseEntity(blockService.getBlockDataList(blockRangeRequest), OK)
    }

    @ApiOperation(
        value = "Returns block by provided hash",
        response = BlockVO::class,
        nickname = "getBlockByHash"
    )
    @GetMapping("{blockHash}")
    fun getBlockByHash(@ApiParam("The block hash") @PathVariable("blockHash") blockHash: String): ResponseEntity<BlockVO> {
        return blockService.findByHash(blockHash)
            ?.let { ResponseEntity(it.toVO(), OK) }
            ?: ResponseEntity(NOT_FOUND)
    }
}