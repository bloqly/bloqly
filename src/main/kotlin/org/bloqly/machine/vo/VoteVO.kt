package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.decode64

@ValueObject
data class VoteVO(
    val validatorId: String,
    val blockHash: String,
    val height: Long,
    val spaceId: String,
    val timestamp: Long,
    val signature: String
) {

    fun toModel(): Vote {
        return Vote(
            validatorId = validatorId,
            blockHash = blockHash,
            height = height,
            spaceId = spaceId,
            timestamp = timestamp,
            signature = signature.decode64()
        )
    }
}
