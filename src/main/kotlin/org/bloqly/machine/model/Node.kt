package org.bloqly.machine.model

import org.bloqly.machine.vo.NodeVO
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class Node(

    @EmbeddedId
    val id: NodeId,

    val addedTime: Long,

    val lastErrorTime: Long? = null,

    val lastSuccessTime: Long? = null,

    val bannedTime: Long? = null) {

    fun getServer(): String {
        return "${this.id.host}:${this.id.port}"
    }

    fun toVO(): NodeVO {

        return NodeVO(
                host = id.host,
                port = id.port,
                addedTime = addedTime,
                lastSuccessTime = lastSuccessTime,
                lastErrorTime = lastErrorTime,
                bannedTime = bannedTime
        )
    }
}