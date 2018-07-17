package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class Properties(val properties: List<Property>)