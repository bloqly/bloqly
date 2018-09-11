package org.bloqly.machine.model

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.bloqly.machine.vo.property.Value
import org.bloqly.machine.vo.transaction.TransactionVO
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "transaction",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["hash"],
            name = "transaction_uq_hash"
        ),
        UniqueConstraint(
            columnNames = ["origin", "timestamp"],
            name = "transaction_uq_origin_timestamp"
        )
    ]
)
@TypeDefs(TypeDef(name = "jsonb", typeClass = JsonBinaryType::class))
data class Transaction(

    @Id
    @GeneratedValue(strategy = AUTO)
    var id: Long? = null,

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

    @Type(type = "jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    val value: List<Value>,

    @Column(nullable = false)
    val transactionType: TransactionType,

    @Column(nullable = false)
    val referencedBlockHash: String,

    @Column(nullable = false)
    val timestamp: Long,

    @Column(nullable = false)
    val signature: String = "",

    @Column(nullable = false)
    val publicKey: String,

    @Column(nullable = false)
    val hash: String = ""
) {

    fun toVO(): TransactionVO {

        return TransactionVO(
            space = spaceId,
            destination = destination,
            self = self,
            key = key,
            value = value,
            transactionType = transactionType,
            referencedBlockHash = referencedBlockHash,
            timestamp = timestamp,
            signature = signature,
            publicKey = publicKey,
            hash = hash
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
        return id?.hashCode() ?: 0
    }
}
