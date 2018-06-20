package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class Round(

    @EmbeddedId
    val id: RoundId,

    val producerId: String,

    val createTime: Long
)