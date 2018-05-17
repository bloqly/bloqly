package org.bloqly.machine.vo

data class NodeVO(
    val host: String,
    val port: Long,
    val addedTime: Long,
    val lastSuccessTime: Long?,
    val lastErrorTime: Long?,
    val bannedTime: Long?
)