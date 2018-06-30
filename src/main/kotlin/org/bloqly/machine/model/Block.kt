package org.bloqly.machine.model

import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.vo.BlockVO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class Block(

    @Id
    val id: String,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val height: Long,

    @Column(nullable = false)
    val round: Long,

    @Column(nullable = false)
    val timestamp: Long,

    @Column(nullable = false)
    val parentId: String,

    @Column
    val proposerId: String,

    @Column
    var txHash: ByteArray? = null,

    @Column
    val validatorTxHash: ByteArray? = null,

    @Lob
    @Column
    val signature: ByteArray? = null
) {

    fun toVO(): BlockVO {

        return BlockVO(
            id = id,
            spaceId = spaceId,
            height = height,
            round = round,
            timestamp = timestamp,
            parentHash = parentId,
            proposerId = proposerId,
            txHash = txHash?.let { EncodingUtils.encodeToString16(txHash) },
            validatorTxHash = validatorTxHash?.let { EncodingUtils.encodeToString16(validatorTxHash) },
            signature = signature?.let { EncodingUtils.encodeToString16(signature) }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
