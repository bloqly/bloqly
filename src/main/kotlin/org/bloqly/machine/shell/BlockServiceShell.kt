package org.bloqly.machine.shell

import org.bloqly.machine.component.EventProcessorService
import org.springframework.stereotype.Service

@Service
class BlockServiceShell(

    private val eventProcessorService: EventProcessorService

) {

    fun init(space: String, baseDir: String): String {

        eventProcessorService.createBlockchain(space, baseDir)

        return "OK"
    }
    
}