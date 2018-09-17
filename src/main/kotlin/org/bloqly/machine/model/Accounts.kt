package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.vo.account.AccountVO

@ValueObject
data class Accounts(val accounts: List<AccountVO>)