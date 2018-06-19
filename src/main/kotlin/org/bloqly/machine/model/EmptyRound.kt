package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class EmptyRound(

    @EmbeddedId
    val id: RoundId,

    val counter: Int,

    val lastMissTime: Long
)