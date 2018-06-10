package org.bloqly.machine.shell

import org.bloqly.machine.service.BlockService
import org.bloqly.machine.util.EncodingUtils
import org.springframework.stereotype.Service

@Service
class BlockServiceShell(
    private val blockService: BlockService
) {

    fun last(space: String): String {
        val lastBlock = blockService.getLastBlockForSpace(space)

        return lastBlock.id
    }

    fun exportFirst(space: String): String {

        val firstBlockJSON = blockService.exportFirst(space)

        return EncodingUtils.encodeToString16(firstBlockJSON.toByteArray())
    }

    fun importFirst(blockEncoded: String): String {

        val firsBlockJSON = String(EncodingUtils.decodeFromString16(blockEncoded))

        blockService.importFirst(firsBlockJSON)

        return "OK"
    }
}