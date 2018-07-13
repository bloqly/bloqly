package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class BlockCandidateId(

    val spaceId: String,

    val height: Long,

    val round: Long,

    val producerId: String

) : Serializable {

    constructor(block: Block) : this(
        spaceId = block.spaceId,
        height = block.height,
        round = block.round,
        producerId = block.producerId
    )
}