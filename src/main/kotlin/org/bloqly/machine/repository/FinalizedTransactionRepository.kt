package org.bloqly.machine.repository

import org.bloqly.machine.model.FinalizedTransaction
import org.springframework.data.repository.CrudRepository

interface FinalizedTransactionRepository : CrudRepository<FinalizedTransaction, Long>