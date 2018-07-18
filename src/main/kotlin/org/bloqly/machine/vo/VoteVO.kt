package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.decode64

@ValueObject
data class VoteVO(
    val publicKey: String,
    val blockHash: String,
    val height: Long,
    val spaceId: String,
    val timestamp: Long,
    val signature: String
) {

    fun toModel(validator: Account): Vote {
        return Vote(
            validator = validator,
            blockHash = blockHash,
            height = height,
            spaceId = spaceId,
            timestamp = timestamp,
            signature = signature.decode64()
        )
    }
}
