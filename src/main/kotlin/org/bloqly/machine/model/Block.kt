package org.bloqly.machine.model

import org.bloqly.machine.vo.BlockVO
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "block",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["hash", "libHash"]),
        UniqueConstraint(columnNames = ["hash", "parentHash"]),
        UniqueConstraint(columnNames = ["spaceId", "producerId", "height"]),
        UniqueConstraint(columnNames = ["spaceId", "producerId", "round"])
    ]
)
data class Block(

    @Id
    @GeneratedValue(strategy = AUTO)
    var id: Long? = null,

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

    @ManyToMany(cascade = [CascadeType.PERSIST], fetch = FetchType.LAZY)
    @JoinTable(
        name = "block_transactions",
        joinColumns = [JoinColumn(name = "block_id")],
        inverseJoinColumns = [JoinColumn(name = "transaction_id")]
    )
    val transactions: List<Transaction> = listOf(),

    @ManyToMany(cascade = [CascadeType.PERSIST], fetch = FetchType.LAZY)
    @JoinTable(
        name = "block_votes",
        joinColumns = [JoinColumn(name = "block_id")],
        inverseJoinColumns = [JoinColumn(name = "vote_id")]
    )
    val votes: List<Vote> = listOf(),

    @Column(nullable = false, unique = true)
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
