package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class PropertyId(

    val spaceId: String,

    val self: String,

    val target: String,

    val key: String

) : Serializable
