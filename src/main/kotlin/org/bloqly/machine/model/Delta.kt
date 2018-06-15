package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class Delta(
    val spaceId: String,
    val localHeight: Long,
    val remoteHeight: Long
) {
    fun isStale(): Boolean = localHeight < remoteHeight
}