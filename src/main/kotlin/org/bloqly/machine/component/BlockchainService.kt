package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.model.PropertyContext
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractExecutorService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.Instant

@Service
@Transactional
class BlockchainService(
    private val blockService: BlockService,
    private val contractExecutorService: ContractExecutorService,
    private val propertyService: PropertyService,
    private val contractService: ContractService,
    private val spaceRepository: SpaceRepository,
    private val transactionService: TransactionService,
    private val blockRepository: BlockRepository,
    private val transactionProcessor: TransactionProcessor
) {
    fun createBlockchain(spaceId: String, baseDir: String) {

        blockService.ensureSpaceEmpty(spaceId)

        val contractBody = File(baseDir).list()
            .filter {
                it.endsWith(".js")
            }
            .map { fileName ->
                val source = File("$baseDir/$fileName").readText()
                val extension = fileName.substringAfterLast(".")
                val header = FileUtils.getResourceAsString("/headers/header.$extension")
                header + source
            }.reduce { str, acc -> str + "\n" + acc }

        val initProperties = contractExecutorService.invokeFunction("init", contractBody)

        val rootId = initProperties.find { it.key == "root" }!!.value.toString()

        propertyService.updateProperties(spaceId, Application.DEFAULT_SELF, initProperties)

        spaceRepository.save(Space(id = spaceId, creatorId = rootId))

        val timestamp = Instant.now().toEpochMilli()

        val validatorTxHash = ByteArray(0)
        val contractBodyHash = CryptoUtils.hash(contractBody).encode16()

        val transaction = transactionService.createTransaction(
            space = spaceId,
            originId = rootId,
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
            txHash = null,
            validatorTxHash = validatorTxHash,
            round = 0,
            transactions = listOf(transaction)
        )

        val propertyContext = PropertyContext(propertyService, contractService)
        transactionProcessor.processTransaction(transaction, propertyContext)
        propertyContext.commit()

        firstBlock.txHash = CryptoUtils.digestTransactions(listOf(transaction))
        blockRepository.save(firstBlock)
    }

    fun isActualTransaction(tx: Transaction, depth: Int): Boolean {

        return blockRepository.findByHash(tx.referencedBlockHash)
            ?.let { referencedBlock ->
                val referencingBlock = blockRepository.findBlockByLibHash(referencedBlock.hash) ?: return false

                val lib = blockService.getLIBForSpace(tx.spaceId)

                lib.height - referencingBlock.height <= depth
            } ?: false
    }
}