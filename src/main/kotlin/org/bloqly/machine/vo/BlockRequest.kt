package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class BlockRequest(
    val spaceId: String,
    val startHeight: Long,
    val endHeight: Long
) {
    fun isStale(): Boolean = startHeight < endHeight - 1
}