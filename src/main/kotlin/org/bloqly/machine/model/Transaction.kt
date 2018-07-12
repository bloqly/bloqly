package org.bloqly.machine.model

import org.bloqly.machine.util.encode64
import org.bloqly.machine.vo.TransactionVO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.Table

@Entity
@Table(name = "transaction")
data class Transaction(

    @Id
    val id: String,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val origin: String,

    @Column(nullable = false)
    val destination: String,

    @Column(nullable = false)
    var self: String,

    @Column(nullable = true)
    var key: String? = null,

    @Lob
    @Column(nullable = false)
    val value: ByteArray,

    @Column(nullable = false)
    val transactionType: TransactionType,

    @Column(nullable = false)
    val referencedBlockId: String,

    @Column(nullable = false)
    val timestamp: Long,

    @Column(nullable = false)
    val signature: ByteArray,

    @Column(nullable = false)
    val publicKey: String
) {

    fun toVO(): TransactionVO {

        return TransactionVO(
            id = id,
            space = spaceId,
            destination = destination,
            self = self,
            key = key,
            value = value.encode64(),
            transactionType = transactionType,
            referencedBlockId = referencedBlockId,
            timestamp = timestamp,
            signature = signature.encode64(),
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
