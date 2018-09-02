package org.bloqly.machine.vo.block

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class BlockDataList(
    val blocks: List<BlockData>
)