package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

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

    @Column(nullable = false)
    var signature: ByteArray

) {

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
