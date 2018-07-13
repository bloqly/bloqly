package org.bloqly.machine.model

import org.bloqly.machine.util.encode16
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
import javax.persistence.Lob
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table(name = "block")
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
    val proposerId: String,

    @Column
    var txHash: ByteArray? = null,

    @Column(nullable = false)
    val validatorTxHash: ByteArray,

    @Lob
    @Column(nullable = false)
    val signature: ByteArray,

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
            proposerId = proposerId,
            txHash = txHash?.encode16(),
            validatorTxHash = validatorTxHash.encode16(),
            signature = signature.encode16(),
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
