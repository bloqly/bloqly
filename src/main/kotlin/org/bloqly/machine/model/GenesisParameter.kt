package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisParameter(
    val target: String,
    val key: String,
    val value: Any
)