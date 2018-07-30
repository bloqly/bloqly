package org.bloqly.machine.model

import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "account",
    uniqueConstraints = [
        (UniqueConstraint(
            columnNames = ["accountId", "nonce"],
            name = "uq_account_account_id_nonce"
        ))
    ]
)
data class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val accountId: String,

    @Column(nullable = false)
    val nonce: Long = 0,

    @Column
    var publicKey: String? = null,

    @Column
    var privateKeyEncoded: ByteArray? = null,

    @Column
    var salt: ByteArray? = null
) {

    var privateKey: String
        get():String {
            return privateKeyEncoded!!.encode16()
        }
        set(value) {
            privateKeyEncoded = value.decode16()
        }

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
