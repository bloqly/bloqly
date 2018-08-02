package org.bloqly.machine.controller.data

import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.controller.exception.NotFoundException
import org.bloqly.machine.model.PropertyValue
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/data/properties")
class PropertyController(
    private val blockProcessor: BlockProcessor
) {

    @GetMapping
    fun getProperty(
        @RequestParam("space") spaceId: String,
        @RequestParam("self") self: String,
        @RequestParam("target") target: String,
        @RequestParam("key") key: String
    ): PropertyValue {
        val value = blockProcessor.getLastPropertyValue(key, target) ?: throw NotFoundException()
        return PropertyValue.of(value)
    }
}