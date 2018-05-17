package org.bloqly.machine.repository

import org.bloqly.machine.model.Account
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface AccountRepository : CrudRepository<Account, String> {

    fun findByPublicKey(publicKey: String): Optional<Account>

}
