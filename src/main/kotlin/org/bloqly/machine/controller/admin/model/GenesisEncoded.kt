package org.bloqly.machine.controller.admin.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisEncoded(
    val genesis: String
)