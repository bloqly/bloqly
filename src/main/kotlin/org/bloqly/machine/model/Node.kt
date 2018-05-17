package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
class Node(

    @EmbeddedId
    val id: NodeId,

    val addedTime: Long,

    val lastErrorTime: Long? = null,

    val lastSuccessTime: Long? = null,

    val bannedTime: Long? = null) {

    fun getServer(): String {
        return "${this.id.host}:${this.id.port}"
    }
}