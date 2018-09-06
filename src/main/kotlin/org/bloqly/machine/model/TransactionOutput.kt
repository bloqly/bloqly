package org.bloqly.machine.model

import org.bloqly.machine.vo.transaction.TransactionOutputVO
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
data class TransactionOutput(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    @Column(nullable = false)
    val blockHash: String,

    @Column(nullable = false)
    val transactionHash: String,

    @Column(nullable = false, columnDefinition = "text")
    val output: String
) {
    fun toVO(): TransactionOutputVO =
        TransactionOutputVO(
            blockHash = blockHash,
            transactionHash = transactionHash,
            output = output
        )
}