package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class EntityEventId(
    val entityId: String,
    val source: String
) : Serializable