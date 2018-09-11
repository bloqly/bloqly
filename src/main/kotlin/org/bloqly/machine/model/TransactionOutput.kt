package org.bloqly.machine.model

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.bloqly.machine.vo.property.PropertyValue
import org.bloqly.machine.vo.transaction.TransactionOutputVO
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "transaction_output",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["blockHash", "transactionHash"],
            name = "tx_uq_block_hash_tx_hash"
        )
    ]
)
@TypeDefs(TypeDef(name = "jsonb", typeClass = JsonBinaryType::class))
data class TransactionOutput(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    @Column(nullable = false)
    val blockHash: String,

    @Column(nullable = false)
    val transactionHash: String,

    @Type(type = "jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    val output: List<PropertyValue>
) {
    fun toVO(): TransactionOutputVO =
        TransactionOutputVO(
            blockHash = blockHash,
            transactionHash = transactionHash,
            output = output
        )
}