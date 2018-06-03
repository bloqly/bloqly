package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.util.EncodingUtils
import org.springframework.stereotype.Service

@Service
@Suppress("unused")
class BlockServiceShell(
    private val eventProcessorService: EventProcessorService,
    private val spaceRepository: SpaceRepository,
    private val objectWriter: ObjectWriter,
    private val blockService: BlockService
) {

    fun init(space: String, baseDir: String): String {

        eventProcessorService.createBlockchain(space, baseDir)

        return "OK"
    }

    fun spaces(): String {

        val spaces = spaceRepository.findAll()

        return objectWriter.writeValueAsString(spaces)
    }

    fun last(space: String): String {
        val lastBlock = blockService.getLastBlockForSpace(space)

        return lastBlock.id
    }

    fun exportFirst(space: String): String {

        val firstBlockJSON = blockService.exportFirst(space)

        return EncodingUtils.encodeToString16(firstBlockJSON.toByteArray())
    }

    fun importFirst(blockEncoded: String): String {

        val firsBlockJSON = EncodingUtils.decodeFromString16(blockEncoded).toString()

        blockService.importFirst(firsBlockJSON)

        return "OK"
    }
}