package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class RoundId (
    val spaceId: String,
    val height: Long
) : Serializable