package org.bloqly.machine.vo.genesis

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisEncoded(
    val genesis: String
)