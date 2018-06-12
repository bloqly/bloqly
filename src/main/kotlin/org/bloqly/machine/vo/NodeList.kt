package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class NodeList(
    val nodes: List<NodeVO>
)