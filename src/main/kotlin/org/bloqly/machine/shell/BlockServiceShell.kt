package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.repository.SpaceRepository
import org.springframework.stereotype.Service

@Service
class BlockServiceShell(

    private val eventProcessorService: EventProcessorService,
    private val spaceRepository: SpaceRepository,
    private val objectWriter: ObjectWriter

) {

    fun init(space: String, baseDir: String): String {

        eventProcessorService.createBlockchain(space, baseDir)

        return "OK"
    }

    fun spaces(): String {

        val spaces = spaceRepository.findAll()

        return objectWriter.writeValueAsString(spaces)
    }
}