package org.bloqly.machine.vo.property

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class PropertyRequest(
    var space: String = DEFAULT_SPACE,
    var self: String = DEFAULT_SELF,
    val key: String,
    val target: String,
    var finalized: Boolean = false
)