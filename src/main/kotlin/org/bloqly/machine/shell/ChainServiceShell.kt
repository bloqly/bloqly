package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.component.ResetService
import org.bloqly.machine.repository.SpaceRepository
import org.springframework.stereotype.Service

@Service
class ChainServiceShell(
    private val eventProcessorService: EventProcessorService,
    private val spaceRepository: SpaceRepository,
    private val objectWriter: ObjectWriter,
    private val resetService: ResetService
) {

    fun init(space: String, baseDir: String): String {

        eventProcessorService.createBlockchain(space, baseDir)

        return "OK"
    }

    fun reset() {
        resetService.reset()
    }

    fun spaces(): String {

        val spaces = spaceRepository.findAll()

        return objectWriter.writeValueAsString(spaces)
    }
}