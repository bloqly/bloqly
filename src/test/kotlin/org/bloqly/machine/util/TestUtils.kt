package org.bloqly.machine.util

import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType

object TestUtils {

    const val FAKE_DATA = "fake_data"

    const val TEST_BLOCK_BASE_DIR = "src/test/resources/blocks/currency-js"

    fun createTransaction(origin: String, destination: String, value: ByteArray): Transaction =
            Transaction(

                    origin = origin,

                    destination = destination,

                    value = value,

                    id = "",

                    space = "",

                    transactionType = TransactionType.CALL,

                    referencedBlockId = "",

                    timestamp = 0,

                    signature = ByteArray(0),

                    publicKey = ""
            )
}