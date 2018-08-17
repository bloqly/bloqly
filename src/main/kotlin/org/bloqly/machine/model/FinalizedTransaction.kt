package org.bloqly.machine.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "finalized_transaction",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["transaction_id", "block_id"],
            name = "fin_tx_uq_tx_id_block_id"
        ),
        UniqueConstraint(
            columnNames = ["transaction_id"],
            name = "fin_tx_uq_tx_id"
        )
    ]
)
data class FinalizedTransaction(

    @Id
    @GeneratedValue(strategy = AUTO)
    var id: Long? = null,

    @ManyToOne(optional = false)
    val transaction: Transaction,

    @ManyToOne(optional = false)
    val block: Block
)
