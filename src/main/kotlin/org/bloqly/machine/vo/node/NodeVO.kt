package org.bloqly.machine.vo.node

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId

@ValueObject
data class NodeVO(
    val host: String,
    val port: Int,
    val addedTime: Long,
    val lastSuccessTime: Long?,
    val lastErrorTime: Long?,
    val bannedTime: Long?
) {

    fun toModel(): Node {

        return Node(
                id = NodeId(host = host, port = port),

                addedTime = addedTime,
                lastErrorTime = lastErrorTime,
                lastSuccessTime = lastSuccessTime,
                bannedTime = bannedTime
        )
    }
}