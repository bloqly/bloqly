package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.decode16

@ValueObject
data class VoteVO(
    val publicKey: String,
    val blockHash: String,
    val height: Long,
    val spaceId: String,
    val timestamp: Long,
    val signature: String
) {

    fun getUID(): String = "$publicKey:$blockHash:$spaceId:$height:$timestamp"

    fun toModel(): Vote {
        return Vote(
            publicKey = publicKey,
            blockHash = blockHash,
            height = height,
            spaceId = spaceId,
            timestamp = timestamp,
            signature = signature.decode16()
        )
    }
}
