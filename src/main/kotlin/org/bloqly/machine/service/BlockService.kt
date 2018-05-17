package org.bloqly.machine.service

import org.bloqly.machine.component.CryptoService
import org.bloqly.machine.model.Block
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class BlockService(

    private val cryptoService: CryptoService,
    private val accountRepository: AccountRepository

) {

    fun newBlock(space: String,
                 height: Long,
                 timestamp: Long,
                 parentHash: String,
                 proposerId: String,
                 txHash: ByteArray,
                 validatorTxHash: ByteArray): Block {

        val accountOpt = accountRepository
                .findById(proposerId)
                .filter { it.privateKey != null }

        return accountOpt.map { proposer ->

            val dataToSign = cryptoService.digest(
                    arrayOf(
                            space.toByteArray(),
                            EncodingUtils.longToBytes(height),
                            EncodingUtils.longToBytes(timestamp),
                            parentHash.toByteArray(),
                            proposerId.toByteArray(),
                            txHash,
                            validatorTxHash
                    )
            )

            val privateKey = decodeFromString(proposer.privateKey)
            val signature = cryptoService.sign(privateKey, dataToSign)
            val blockHash = cryptoService.digest(signature)
            val blockId = EncodingUtils.encodeToString(blockHash)

            Block(
                    id = blockId,
                    space = space,
                    height = height,
                    timestamp = timestamp,
                    parentHash = parentHash,
                    proposerId = proposerId,
                    txHash = txHash,
                    validatorTxHash = validatorTxHash,
                    signature = signature
            )
        }.orElseThrow()
    }
}