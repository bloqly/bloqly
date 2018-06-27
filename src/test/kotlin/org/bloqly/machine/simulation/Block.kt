package org.bloqly.machine.simulation

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

data class Block(
    val proposerId: Int? = -1,
    val parent: Block? = null,
    val round: Int,
    val height: Long,
    val proofOfLock: Set<Vote>? = null
) {
    override fun toString(): String {
        return "Block(proposerId=$proposerId, parent=$parent, round=$round, height=$height)"
    }

    override fun equals(other: Any?): Boolean {
        val otherBlock = other as Block?
        return EqualsBuilder()
            .append(proposerId, otherBlock?.proposerId)
            .append(round, otherBlock?.round)
            .append(height, otherBlock?.height)
            .append(proofOfLock, otherBlock?.proofOfLock)
            .build()
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
            .append(proposerId)
            .append(round)
            .append(height)
            .append(proofOfLock)
            .build()
    }
}