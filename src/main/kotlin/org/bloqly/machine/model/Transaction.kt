package org.bloqly.machine.model

import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.vo.TransactionVO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class Transaction(

    @Id
    val id: String,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val origin: String,

    @Column(nullable = false)
    val destination: String,

    @Column(nullable = true)
    var self: String? = null,

    @Column(nullable = true)
    var key: String? = null,

    @Lob
    @Column(nullable = false)
    val value: ByteArray,

    @Column(nullable = true)
    val output: ByteArray? = null,

    @Column(nullable = false)
    val transactionType: TransactionType,

    @Column(nullable = false)
    val referencedBlockId: String,

    @Column(nullable = true)
    var containingBlockId: String? = null,

    @Column(nullable = false)
    val timestamp: Long,

    @Column(nullable = false)
    val signature: ByteArray,

    @Column(nullable = false)
    val publicKey: String
) {

    fun toVO(): TransactionVO {

        val value = EncodingUtils.encodeToString64(value)
        val signature = EncodingUtils.encodeToString64(signature)

        return TransactionVO(
            id = id,
            space = spaceId,
            destination = destination,
            self = self,
            key = key,
            value = value,
            transactionType = transactionType,
            referencedBlockId = referencedBlockId,
            containingBlockId = containingBlockId,
            timestamp = timestamp,
            signature = signature,
            publicKey = publicKey
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
