package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
data class VoteId(

    @Column(nullable = false)
    val validatorId: String,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val height: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val voteType: VoteType = VoteType.VOTE

) : Serializable {

    override fun toString(): String {
        return "$validatorId:$spaceId:$height"
    }
}