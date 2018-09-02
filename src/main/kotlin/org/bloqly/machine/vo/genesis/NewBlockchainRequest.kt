package org.bloqly.machine.vo.genesis

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class NewBlockchainRequest(
    val space: String,
    val path: String,
    val passphrase: String
)