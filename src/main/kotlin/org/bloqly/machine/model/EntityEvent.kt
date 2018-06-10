package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class EntityEvent(

    @EmbeddedId
    val entityEventId: EntityEventId,

    val timestamp: Long
)