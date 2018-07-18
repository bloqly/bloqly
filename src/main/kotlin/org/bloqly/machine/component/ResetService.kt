package org.bloqly.machine.component

import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.repository.NodeRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.TimeUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ResetService(
    private val contractRepository: ContractRepository,
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val nodeRepository: NodeRepository,
    private val voteRepository: VoteRepository,
    private val transactionOutputRepository: TransactionOutputRepository
) {

    fun reset() {
        contractRepository.deleteAll()
        propertyRepository.deleteAll()
        blockRepository.deleteAll()
        spaceRepository.deleteAll()
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        nodeRepository.deleteAll()
        voteRepository.deleteAll()
        transactionOutputRepository.deleteAll()
        TimeUtils.reset()
    }
}