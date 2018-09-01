package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class Accounts(val accounts: List<Account>)