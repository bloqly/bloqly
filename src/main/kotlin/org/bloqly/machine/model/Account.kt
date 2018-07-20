package org.bloqly.machine.model

import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

// TODO introduce nonce
@Entity
data class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val accountId: String,

    @Column
    var publicKey: String? = null,

    @Column
    var privateKeyEncoded: ByteArray? = null
) {

    var privateKey: String
        get():String {
            return privateKeyEncoded!!.encode16()
        }
        set(value) {
            privateKeyEncoded = value.decode16()
        }

    var privateKeyBytes: ByteArray
        get():ByteArray {
            return privateKeyEncoded!!
        }
        set(value) {
            privateKeyEncoded = value
        }

    fun hasKey(): Boolean = privateKeyEncoded != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
