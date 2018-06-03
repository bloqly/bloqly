package org.bloqly.machine.model

import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.vo.TransactionVO
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class Transaction(

    @Id
    val id: String,

    val space: String,

    val origin: String,

    val destination: String,

    var self: String? = null,

    var key: String? = null,

    @Lob
    val value: ByteArray,

    val output: ByteArray? = null,

    val transactionType: TransactionType,

    val referencedBlockId: String,

    var containingBlockId: String? = null,

    val timestamp: Long,

    val signature: ByteArray,

    val publicKey: String
) {

    fun toVO(): TransactionVO {

        val value = EncodingUtils.encodeToString64(value)
        val signature = EncodingUtils.encodeToString64(signature)

        return TransactionVO(
            id = id,
            space = space,
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
