package org.bloqly.machine.model

data class InvocationContext(
    val functionName: String,
    val caller: String,
    val callee: String,
    val contract: Contract
)
