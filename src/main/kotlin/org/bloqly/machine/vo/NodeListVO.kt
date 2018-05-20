package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class NodeListVO(
    val nodes: List<NodeVO>
)