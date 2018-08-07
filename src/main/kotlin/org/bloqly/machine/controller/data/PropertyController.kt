package org.bloqly.machine.controller.data

import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.controller.data.model.PropertyRequest
import org.bloqly.machine.controller.exception.NotFoundException
import org.bloqly.machine.model.PropertyValue
import org.bloqly.machine.repository.PropertyService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/data/properties")
class PropertyController(
    private val blockProcessor: BlockProcessor,
    private val propertyService: PropertyService
) {

    @PostMapping("/search")
    fun getPropertyCandidate(@RequestBody request: PropertyRequest): PropertyValue {

        val value = if (request.finalized) {
            propertyService.getPropertyValue(request.space, request.self, request.target, request.key)
        } else {
            blockProcessor.getLastPropertyValue(request.space, request.self, request.target, request.key)
        } ?: throw NotFoundException()

        return PropertyValue.of(value)
    }
}