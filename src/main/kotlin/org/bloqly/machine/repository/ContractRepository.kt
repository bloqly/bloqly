package org.bloqly.machine.repository

import org.bloqly.machine.model.Contract
import org.springframework.data.repository.CrudRepository

interface ContractRepository : CrudRepository<Contract, String>
