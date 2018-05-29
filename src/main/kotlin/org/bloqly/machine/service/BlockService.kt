package org.bloqly.machine.service

import org.bloqly.machine.model.Block
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class BlockService(
    private val accountRepository: AccountRepository,
    private val blockRepository: BlockRepository
) {

    fun newBlock(
        space: String,
        height: Long,
        timestamp: Long,
        parentHash: String,
        proposerId: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray
    ): Block {

        val accountOpt = accountRepository
            .findById(proposerId)
            .filter { it.privateKey != null }

        return accountOpt.map { proposer ->

            val dataToSign = CryptoUtils.digest(
                arrayOf(
                    space.toByteArray(),
                    EncodingUtils.longToBytes(height),
                    EncodingUtils.longToBytes(timestamp),
                    parentHash.toByteArray(),
                    proposerId.toByteArray(),
                    txHash ?: ByteArray(0),
                    validatorTxHash
                )
            )

            val privateKey = decodeFromString16(proposer.privateKey)
            val signature = CryptoUtils.sign(privateKey, dataToSign)
            val blockHash = CryptoUtils.digest(signature)
            val blockId = EncodingUtils.encodeToString16(blockHash)

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

    fun getLastBlockForSpace(space: String): Block {
        return blockRepository.findFirstBySpaceOrderByHeightDesc(space)
    }
}