package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class VoteId(

    @Column(nullable = false)
    val validatorId: String,

    @Column(nullable = false)
    val spaceId: String,

    @Column(nullable = false)
    val height: Long,

    @Column(nullable = false)
    val round: Long

) : Serializable {

    override fun toString(): String {
        return "$validatorId:$spaceId:$height:$round"
    }
}