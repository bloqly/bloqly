package org.bloqly.machine.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.GenesisParameters
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.TestUtils.TEST_BLOCK_BASE_DIR
import org.bloqly.machine.vo.TransactionVO
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.annotation.PostConstruct
import javax.transaction.Transactional

@Component
@Transactional
class TestService(
    private val contractRepository: ContractRepository,
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val eventProcessorService: EventProcessorService,
    private val transactionService: TransactionService,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper) {

    private lateinit var genesisParameters: GenesisParameters

    @PostConstruct
    @Suppress("unused")
    fun init() {

        genesisParameters = eventProcessorService.readGenesis(TEST_BLOCK_BASE_DIR)
    }

    fun cleanup() {
        contractRepository.deleteAll()
        propertyRepository.deleteAll()
        blockRepository.deleteAll()
        spaceRepository.deleteAll()
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
    }

    fun getRoot(): Account = genesisParameters.root

    fun getUser(): Account = genesisParameters.users!!.first()

    fun getValidator(n: Int): Account = genesisParameters.validators!![n]

    fun createBlockchain() {

        val accountsString = FileUtils.getResourceAsString("/accounts.json")

        val accountsObject = objectMapper.readValue(accountsString, Accounts::class.java)

        accountsObject.accounts.forEach { account ->
            accountService.importAccount(account.privateKey!!)
        }

        eventProcessorService.createBlockchain(Application.DEFAULT_SPACE, TEST_BLOCK_BASE_DIR)
    }

    fun newTransaction(): TransactionVO {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(DEFAULT_SPACE)

        val root = accountRepository.findById(getRoot().id).orElseThrow()
        val user = accountRepository.findById(getUser().id).orElseThrow()

        val transaction = transactionService.newTransaction(
                space = DEFAULT_SPACE,
                originId = root.id,
                destinationId = user.id,
                value = writeLong("1"),
                transactionType = TransactionType.CALL,
                referencedBlockId = lastBlock.id,
                timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()
        )

        return transaction.toVO()
    }

    @ValueObject
    private data class Accounts(val accounts: List<Account>)
}
