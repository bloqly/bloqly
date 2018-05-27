package org.bloqly.machine.model

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

    val containingBlockId: String? = null,

    val timestamp: Long,

    val signature: ByteArray,

    val publicKey: String

) {

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
