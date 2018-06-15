package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class BlockDataList(
    val blocks: List<BlockData>
)