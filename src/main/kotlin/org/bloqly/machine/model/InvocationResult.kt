package org.bloqly.machine.model

data class InvocationResult(
    val invocationResultType: InvocationResultType,
    val result: String? = null
)