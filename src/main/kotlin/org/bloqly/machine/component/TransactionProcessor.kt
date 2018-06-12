package org.bloqly.machine.component

import org.apache.commons.lang3.StringUtils
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.GenesisParameter
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.ParameterUtils
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
        parameters: List<GenesisParameter>
    ) {

        require(StringUtils.isNotEmpty(body)) {
            "Contract body can not be empty"
        }

        val owner = parameters.stream()
            .filter { it.key == "root" }
            .findFirst()
            .orElseThrow()

        val contract = Contract(self, space, owner.value.toString(), body)

        val properties = parameters.map { (target, key, value) ->
            Property(
                id = PropertyId(
                    space = space,
                    self = self,
                    target = target,
                    key = key
                ),
                value = ParameterUtils.writeValue(value)
            )
        }

        propertyService.updateProperties(properties)

        contractRepository.save(contract)
    }
}