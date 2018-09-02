package org.bloqly.machine.vo.node

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class NodeList(
    val nodes: List<NodeVO>
)