package org.bloqly.machine.model

import org.bloqly.machine.vo.property.PropertyValue

data class InvocationResult(
    val invocationResultType: InvocationResultType,
    val output: List<PropertyValue> = listOf()
) {
    constructor(result: InvocationResultType) : this(result, listOf())

    fun isOK(): Boolean = invocationResultType == InvocationResultType.SUCCESS
}