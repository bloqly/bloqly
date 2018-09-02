package org.bloqly.machine.vo.block

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class BlockRequest(
    val spaceId: String,
    var startHeight: Long = 0,
    var endHeight: Long = 0
) {
    fun isStale(): Boolean = startHeight < endHeight - 1
}