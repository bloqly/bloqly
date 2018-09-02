package org.bloqly.machine.vo.account

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class PublicKeysImportRequest(
    val publicKeys: List<String>
)