package org.bloqly.machine.component

import org.apache.commons.lang3.StringUtils
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class TransactionProcessor(
    private val contractRepository: ContractRepository,
    private val accountRepository: AccountRepository,
    private val contractService: ContractService
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

        contractService.invokeContract("init", self, owner, self, byteArrayOf())
    }

    fun processTransaction(transaction: Transaction) {

        accountRepository.insertAccountId(transaction.origin)
        accountRepository.insertAccountId(transaction.destination)
        accountRepository.insertAccountId(transaction.self)

        val originId = transaction.origin

        val origin = accountRepository.findById(originId).orElseThrow()

        val publicKey = EncodingUtils.decodeFromString16(transaction.publicKey)

        val publicKeyHash = EncodingUtils.encodeToString16(CryptoUtils.digest(publicKey))

        if (origin.publicKey == null && publicKeyHash == origin.id) {
            accountRepository.save(origin.copy(publicKey = transaction.publicKey))
        }

        if (transaction.transactionType == TransactionType.CREATE) {

            createContract(
                space = transaction.spaceId,
                self = transaction.self!!,
                body = String(transaction.value),
                owner = transaction.destination
            )
        }
    }
}