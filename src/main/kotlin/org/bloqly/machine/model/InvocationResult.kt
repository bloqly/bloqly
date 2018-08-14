package org.bloqly.machine.model

data class InvocationResult(
    val invocationResultType: InvocationResultType,
    val output: List<Property> = listOf()
) {
    constructor(result: InvocationResultType) : this(result, listOf())

    fun isOK(): Boolean = invocationResultType == InvocationResultType.SUCCESS
}