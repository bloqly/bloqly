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
    var id: String,

    @Column(nullable = false)
    var space: String,

    @Column(nullable = false)
    var height: Long,

    @Column(nullable = false)
    var timestamp: Long,

    @Column(nullable = false)
    var parentHash: String,

    @Column(nullable = false)
    var proposerId: String,

    @Column
    var txHash: ByteArray?,

    @Column(nullable = false)
    var validatorTxHash: ByteArray,

    @Lob
    @Column(nullable = false)
    var signature: ByteArray
) {

    fun toVO(): BlockVO {

        return BlockVO(
                id = id,
                space = space,
                height = height,
                timestamp = timestamp,
                parentHash = parentHash,
                proposerId = proposerId,
                txHash = EncodingUtils.encodeToString16(txHash),
                validatorTxHash = EncodingUtils.encodeToString16(validatorTxHash),
                signature = EncodingUtils.encodeToString16(signature)
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
