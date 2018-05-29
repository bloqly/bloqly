package org.bloqly.machine.model

data class ContractInvocationContext(
    val functionName: String,
    val caller: String,
    val callee: String,
    val contract: Contract
)
