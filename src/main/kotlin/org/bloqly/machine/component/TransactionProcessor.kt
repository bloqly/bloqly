package org.bloqly.machine.component

import org.apache.commons.lang3.StringUtils
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.service.TransactionService
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class TransactionProcessor(
    private val transactionService: TransactionService,
    private val propertyService: PropertyService,
    private val contractRepository: ContractRepository
) {

    fun createContract(
        space: String,
        self: String,
        body: String,
        owner: String
    ) {

        require(StringUtils.isNotEmpty(body)) {
            "Contract body can not be empty"
        }

        val contract = Contract(self, space, owner, body)

        contractRepository.save(contract)
    }

    fun processTransaction(transaction: Transaction) {

        if (transaction.transactionType == TransactionType.CREATE) {
            createContract(
                space = transaction.space,
                self = transaction.self!!,
                body = String(transaction.value),
                owner = transaction.destination
            )
        }
    }
}