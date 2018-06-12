package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.BlockData

@ValueObject
class BlockDataList(
    val blocks: List<BlockDataVO>
) {
    companion object {

        fun fromBlocks(blocks: List<BlockData>): BlockDataList {
            return BlockDataList(blocks.map { it.toVO() })
        }
    }
}