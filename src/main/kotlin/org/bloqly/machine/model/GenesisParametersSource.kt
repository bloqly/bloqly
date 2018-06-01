package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisParametersSource(
    val genesisParameters: GenesisParameters,
    val source: String
)
