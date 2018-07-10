package org.bloqly.machine.repository

import org.bloqly.machine.model.TransactionOutput
import org.bloqly.machine.model.TransactionOutputId
import org.springframework.data.repository.CrudRepository

interface TransactionOutputRepository : CrudRepository<TransactionOutput, TransactionOutputId>