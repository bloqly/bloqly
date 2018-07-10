package org.bloqly.machine.service

import org.bloqly.machine.model.Contract
import org.bloqly.machine.repository.ContractRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ContractService(
    private val contractRepository: ContractRepository
) {
    fun findById(self: String): Contract? = contractRepository.findById(self).orElse(null)

    fun saveAll(contracts: List<Contract>) {
        contractRepository.saveAll(contracts)
    }
}