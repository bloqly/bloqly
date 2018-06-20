package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Account(

    @Id
    var id: String,

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
        return id.hashCode()
    }
}
