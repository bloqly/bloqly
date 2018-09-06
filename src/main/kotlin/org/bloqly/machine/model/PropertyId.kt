package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class PropertyId(

    val spaceId: String,

    val self: String,

    val target: String,

    val key: String

) : Serializable, Comparable<PropertyId> {

    override fun compareTo(other: PropertyId): Int =
        Comparator.comparing(PropertyId::spaceId)
            .thenComparing(PropertyId::self)
            .thenComparing(PropertyId::target)
            .thenComparing(PropertyId::key)
            .compare(this, other)
}
