package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.FinalizedTransaction
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.FinalizedTransactionRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractExecutorService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.SpaceService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Service
class BlockchainService(
    private val blockService: BlockService,
    private val contractExecutorService: ContractExecutorService,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val spaceService: SpaceService,
    private val transactionService: TransactionService,
    private val blockRepository: BlockRepository,
    private val transactionProcessor: TransactionProcessor,
    private val finalizedTransactionRepository: FinalizedTransactionRepository
) {

    @Transactional
    fun createBlockchain(spaceId: String, baseDir: String, passphrase: String) {

        blockService.ensureSpaceEmpty(spaceId)

        val header = FileUtils.getResourceAsString("/headers/header.js")

        val source = File(baseDir).list()
            .filter { it.endsWith(".js") }
            .map { fileName -> File("$baseDir/$fileName").readText() }
            .reduce { str, acc -> str + "\n" + acc }

        val contractBody = header + "\n" + source

        val initProperties = contractExecutorService.invokeFunction("init", contractBody)

        val rootId = initProperties.find { it.key == "root" }!!.value.toString()

        propertyService.updateProperties(spaceId, Application.DEFAULT_SELF, initProperties)

        spaceService.save(Space(id = spaceId, creatorId = rootId))

        val timestamp = TimeUtils.getCurrentTime()

        val validatorTxHash = ByteArray(0)
        val contractBodyHash = CryptoUtils.hash(contractBody).encode16()

        val tx = transactionService.createTransaction(
            space = spaceId,
            originId = rootId,
            passphrase = passphrase,
            destinationId = Application.DEFAULT_SELF,
            self = Application.DEFAULT_SELF,
            key = null,
            value = contractBody.toByteArray(),
            transactionType = TransactionType.CREATE,
            referencedBlockHash = "",
            timestamp = timestamp
        )

        val firstBlock = blockService.newBlock(
            spaceId = spaceId,
            height = 0,
            weight = 0,
            diff = 0,
            timestamp = timestamp,
            parentHash = contractBodyHash,
            producerId = rootId,
            passphrase = passphrase,
            txHash = null,
            validatorTxHash = validatorTxHash,
            round = 0,
            transactions = listOf(tx)
        )

        val propertyContext = PropertyContext(propertyService, contractService)
        val result = transactionProcessor.processTransaction(tx, propertyContext)

        require(result.isOK()) {
            "Could not process genesis block transaction ${tx.toVO()}"
        }

        propertyContext.commit()

        firstBlock.txHash = CryptoUtils.hashTransactions(listOf(tx)).encode16()
        val savedFirstBlock = blockRepository.save(firstBlock)

        finalizedTransactionRepository.save(
            FinalizedTransaction(
                transaction = tx,
                block = savedFirstBlock
            )
        )
    }
}