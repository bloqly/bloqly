package org.bloqly.machine.service

import org.bloqly.machine.model.Contract
import org.bloqly.machine.repository.ContractRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional

@Service
class ContractService(
    private val contractRepository: ContractRepository
) {
    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun findById(self: String): Contract? = contractRepository.findById(self).orElse(null)

    @Transactional(isolation = SERIALIZABLE)
    fun saveAll(contracts: List<Contract>) {
        contractRepository.saveAll(contracts)
    }
}