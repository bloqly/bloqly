package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.component.EventProcessorService
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.springframework.stereotype.Service

@Service
class BlockServiceShell(

    private val eventProcessorService: EventProcessorService,
    private val spaceRepository: SpaceRepository,
    private val objectWriter: ObjectWriter,
    private val accountService: AccountService

) {

    fun init(space: String, baseDir: String): String {

        eventProcessorService.createBlockchain(space, baseDir)

        return "OK"
    }

    fun spaces(): String {

        val spaces = spaceRepository.findAll()

        return objectWriter.writeValueAsString(spaces)
    }

    fun validators(space: String): String {

        val validators = accountService.getValidatorsForSpace(space)

        validators.forEach { validator ->

            validator.privateKey?.let {
                validator.privateKey = "hidden"
            }
        }

        return "\n" + objectWriter.writeValueAsString(validators)
    }
}