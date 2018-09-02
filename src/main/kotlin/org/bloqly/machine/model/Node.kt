package org.bloqly.machine.model

import org.bloqly.machine.vo.node.NodeVO
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class Node(

    @EmbeddedId
    val id: NodeId,

    val addedTime: Long,

    val lastErrorTime: Long? = null,

    val lastSuccessTime: Long? = null,

    val bannedTime: Long? = null
) {

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