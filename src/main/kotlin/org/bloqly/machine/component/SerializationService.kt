package org.bloqly.machine.component

import org.bloqly.machine.model.Block
import org.bloqly.machine.model.BlockData
import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.vo.BlockDataVO
import org.bloqly.machine.vo.BlockVO
import org.bloqly.machine.vo.NodeVO
import org.bloqly.machine.vo.TransactionListVO
import org.bloqly.machine.vo.TransactionVO
import org.bloqly.machine.vo.VoteVO
import org.springframework.stereotype.Component

@Component
class SerializationService(

    private val cryptoService: CryptoService) {

    fun nodeFromVO(nodeVO: NodeVO): Node {

        return Node(
                id = NodeId(
                        host = nodeVO.host,
                        port = nodeVO.port
                ),

                addedTime = nodeVO.addedTime,
                lastErrorTime = nodeVO.lastErrorTime,
                lastSuccessTime = nodeVO.lastSuccessTime,
                bannedTime = nodeVO.bannedTime
        )
    }

    fun nodeToVO(node: Node): NodeVO {

        return NodeVO(
                host = node.id.host,
                port = node.id.port,
                addedTime = node.addedTime,
                lastSuccessTime = node.lastSuccessTime,
                lastErrorTime = node.lastErrorTime,
                bannedTime = node.bannedTime
        )
    }

    fun transactionFromVO(transactionVO: TransactionVO): Transaction {

        val signature = transactionVO.signature.toByteArray()
        val publicKeyHash = cryptoService.digest(signature)
        val origin = EncodingUtils.encodeToString16(publicKeyHash)
        val transactionType = TransactionType.valueOf(transactionVO.transactionType.name)

        return Transaction(

                id = transactionVO.id,
                space = transactionVO.space,
                origin = origin,
                destination = transactionVO.destination,
                self = transactionVO.self,
                key = transactionVO.key,
                value = EncodingUtils.decodeFromString64(transactionVO.value),
                transactionType = transactionType,
                referencedBlockId = transactionVO.referencedBlockId,
                timestamp = transactionVO.timestamp,
                signature = signature,
                publicKey = transactionVO.publicKey
        )
    }

    fun transactionToVO(transaction: Transaction): TransactionVO {

        return TransactionVO(
                id = transaction.id,
                space = transaction.space,
                destination = transaction.destination,
                self = transaction.self,
                key = transaction.key,
                value = EncodingUtils.encodeToString64(transaction.value),
                transactionType = transaction.transactionType,
                referencedBlockId = transaction.referencedBlockId,
                timestamp = transaction.timestamp,
                signature = EncodingUtils.encodeToString16(transaction.signature),
                publicKey = transaction.publicKey
        )
    }

    fun transactionsToVO(transactions: List<Transaction>): TransactionListVO {
        return TransactionListVO(
                transactions = transactions.map {
                    transactionToVO(it)
                }
        )
    }

    fun voteToVO(vote: Vote): VoteVO {

        return VoteVO(
                validatorId = vote.id.validatorId,
                space = vote.id.space,
                height = vote.id.height,
                blockId = vote.blockId,
                timestamp = vote.timestamp,
                signature = EncodingUtils.encodeToString16(vote.signature)
        )
    }

    fun voteFromVO(voteVO: VoteVO): Vote {

        val voteId = VoteId(
                validatorId = voteVO.validatorId,
                space = voteVO.space,
                height = voteVO.height
        )

        return Vote(
                id = voteId,
                blockId = voteVO.blockId,
                timestamp = voteVO.timestamp,
                signature = EncodingUtils.decodeFromString16(voteVO.signature)
        )
    }

    fun blockToVO(block: Block): BlockVO {

        return BlockVO(
                id = block.id,
                space = block.space,
                height = block.height,
                timestamp = block.timestamp,
                parentHash = block.parentHash,
                proposerId = block.proposerId,
                txHash = EncodingUtils.encodeToString16(block.txHash),
                validatorTxHash = EncodingUtils.encodeToString16(block.validatorTxHash),
                signature = EncodingUtils.encodeToString16(block.signature)
        )
    }

    fun blockFromVO(blockVO: BlockVO): Block {

        return Block(
                id = blockVO.id,
                space = blockVO.space,
                height = blockVO.height,
                timestamp = blockVO.timestamp,
                parentHash = blockVO.parentHash,
                proposerId = blockVO.proposerId,
                txHash = blockVO.txHash.toByteArray(),
                validatorTxHash = blockVO.validatorTxHash.toByteArray(),
                signature = blockVO.signature.toByteArray()
        )
    }

    fun blockDataToVO(blockData: BlockData): BlockDataVO {

        return BlockDataVO(
                block = blockToVO(blockData.block),
                transactions = blockData.transactions.map { transactionToVO(it) },
                votes = blockData.votes.map { voteToVO(it) }
        )
    }

    fun blockDataFromVO(blockDataVO: BlockDataVO): BlockData {

        return BlockData(
                block = blockFromVO(blockDataVO.block),
                transactions = blockDataVO.transactions.map { transactionFromVO(it) },
                votes = blockDataVO.votes.map { voteFromVO(it) }
        )
    }
}