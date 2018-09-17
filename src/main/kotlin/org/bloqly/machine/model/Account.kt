package org.bloqly.machine.model

import org.apache.commons.codec.binary.Hex
import org.bloqly.machine.vo.account.AccountVO
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
        UniqueConstraint(
            columnNames = ["accountId"],
            name = "account_uq_account_id"
        )]
)
data class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    @Column(nullable = false)
    val accountId: String,

    @Column(nullable = false)
    var publicKey: String,

    @Column
    var privateKeyEncoded: ByteArray? = null
) {

    var privateKey: String
        get(): String = Hex.encodeHexString(privateKeyEncoded)
        set(value) {
            privateKeyEncoded = Hex.decodeHex(value)
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

    fun toVO(): AccountVO =
        AccountVO(
            accountId = accountId,
            publicKey = publicKey
        )

    fun toFullVO(): AccountVO =
        AccountVO(
            accountId = accountId,
            publicKey = publicKey,
            privateKeyEncoded = Hex.encodeHexString(privateKeyEncoded)
        )
}
