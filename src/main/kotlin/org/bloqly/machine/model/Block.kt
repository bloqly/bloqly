package org.bloqly.machine.model

import org.bloqly.machine.vo.BlockVO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "block",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["hash", "libHash"],
            name = "block_uq_hash_lib_hash"
        ),
        UniqueConstraint(
            columnNames = ["hash", "parentHash"],
            name = "block_uq_hash_parent_hash"
        ),
        UniqueConstraint(
            columnNames = ["spaceId", "producerId", "height"],
            name = "block_uq_space_producer_height"
        ),
        UniqueConstraint(
            columnNames = ["spaceId", "producerId", "round"],
            name = "block_uq_producer_round"
        ),
        UniqueConstraint(
            columnNames = ["hash"],
            name = "block_uq_hash"
        )
    ],
    indexes = [
        Index(
            columnList = "height DESC, diff DESC, weight DESC, round, hash",
            name = "block_last_block_idx"
        ),
        Index(
            columnList = "spaceId",
            name = "block_space_idx"
        )
    ]
)
data class Block(

    @Id
    @GeneratedValue(strategy = AUTO)
    var id: Long? = null,

    // TODO add Nonnull annotation?
    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val height: Long,

    @Column(nullable = false)
    val weight: Long,

    @Column(nullable = false)
    val diff: Int,

    @Column(nullable = false)
    val round: Long,

    @Column(nullable = false)
    val timestamp: Long,

    // TODO define string column length more precisely
    @Column(nullable = false)
    val parentHash: String,

    @Column(nullable = false)
    val producerId: String,

    @Column
    var txHash: String? = null,

    @Column(nullable = false)
    val validatorTxHash: String,

    @Column(nullable = false)
    val signature: String,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "block_transactions",
        joinColumns = [JoinColumn(name = "block_id")],
        inverseJoinColumns = [JoinColumn(name = "transaction_id")]
    )
    val transactions: List<Transaction> = listOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "block_votes",
        joinColumns = [JoinColumn(name = "block_id")],
        inverseJoinColumns = [JoinColumn(name = "vote_id")]
    )
    val votes: List<Vote> = listOf(),

    @Column(nullable = false)
    val hash: String,

    @Column(nullable = false)
    val libHash: String
) {

    fun toVO(): BlockVO {

        return BlockVO(
            spaceId = spaceId,
            height = height,
            weight = weight,
            diff = diff,
            round = round,
            timestamp = timestamp,
            parentHash = parentHash,
            producerId = producerId,
            txHash = txHash,
            validatorTxHash = validatorTxHash,
            signature = signature,
            hash = hash,
            libHash = libHash
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
