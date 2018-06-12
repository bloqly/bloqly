package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class BlockCandidateId(

    val space: String,

    val height: Long,

    val proposerId: String
) : Serializable