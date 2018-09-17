package org.bloqly.machine.model

import org.bloqly.machine.util.toHex
import org.bloqly.machine.vo.vote.VoteVO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "vote",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["spaceId", "publicKey", "height"],
            name = "vote_uq_space_validator_height"
        ),
        UniqueConstraint(
            columnNames = ["publicKey", "blockHash"],
            name = "vote_uq_validator_block_hash"
        )
    ]
)
data class Vote(

    @Id
    @GeneratedValue(strategy = AUTO)
    var id: Long? = null,

    @Column(nullable = false)
    val publicKey: String,

    @Column(nullable = false)
    val blockHash: String,

    // height of the block this vote refers to
    @Column(nullable = false)
    val height: Long,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val timestamp: Long,

    @Column(nullable = false)
    val signature: ByteArray? = null
) {

    fun toVO(): VoteVO {

        return VoteVO(
            publicKey = publicKey,
            blockHash = blockHash,
            height = height,
            spaceId = spaceId,
            timestamp = timestamp,
            signature = signature.toHex()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vote

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}