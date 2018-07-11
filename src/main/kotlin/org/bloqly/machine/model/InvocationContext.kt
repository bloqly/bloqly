package org.bloqly.machine.model

data class InvocationContext(
    val space: String,
    var owner: String? = null,
    val self: String,
    val key: String,
    val caller: String,
    val callee: String
)
