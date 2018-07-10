package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.model.VoteType
import org.bloqly.machine.util.decode64

@ValueObject
data class VoteVO(
    val validatorId: String,
    val spaceId: String,
    val height: Long,
    val voteType: String,
    val blockId: String,
    val timestamp: Long,
    val signature: String,
    val publicKey: String
) {

    fun toModel(): Vote {

        val voteId = VoteId(
            validatorId = validatorId,
            spaceId = spaceId,
            height = height,
            voteType = VoteType.valueOf(voteType)
        )

        return Vote(
            id = voteId,
            blockId = blockId,
            timestamp = timestamp,
            signature = signature.decode64(),
            publicKey = publicKey
        )
    }
}
