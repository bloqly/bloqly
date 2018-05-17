package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
class NodeId(

    val host: String,

    val port: Long

) : Serializable