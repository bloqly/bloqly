package org.bloqly.machine.model

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
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var accountId: String,

    @Column
    var publicKey: String? = null,

    @Column
    var privateKey: String? = null
) {

    fun hasKey(): Boolean = privateKey != null

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
