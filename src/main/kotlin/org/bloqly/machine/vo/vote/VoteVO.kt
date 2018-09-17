package org.bloqly.machine.vo.vote

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.codec.binary.Hex
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Vote

@ValueObject
data class VoteVO(
    val publicKey: String,
    val blockHash: String,
    val height: Long,
    val spaceId: String,
    val timestamp: Long,
    val signature: String
) {

    @JsonIgnore
    fun getUID(): String = "$publicKey:$blockHash:$spaceId:$height:$timestamp"

    fun toModel(): Vote {
        return Vote(
            publicKey = publicKey,
            blockHash = blockHash,
            height = height,
            spaceId = spaceId,
            timestamp = timestamp,
            signature = Hex.decodeHex(signature)
        )
    }
}
