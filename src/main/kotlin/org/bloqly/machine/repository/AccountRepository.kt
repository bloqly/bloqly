package org.bloqly.machine.repository

import org.bloqly.machine.model.Account
import org.springframework.data.repository.CrudRepository

interface AccountRepository : CrudRepository<Account, String>