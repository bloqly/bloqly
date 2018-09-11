package org.bloqly.machine.controller.data

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.bloqly.machine.component.BlockProcessor
import org.bloqly.machine.controller.exception.NotFoundException
import org.bloqly.machine.model.PropertyValue
import org.bloqly.machine.service.PropertyService
import org.bloqly.machine.vo.property.PropertyRequest
import org.bloqly.machine.vo.property.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api(
    value = "/api/v1/data/properties",
    description = "Operations implementing read access to properties",
    consumes = "application/json",
    produces = "application/json"
)
@Profile("server")
@RestController
@RequestMapping("/api/v1/data/properties")
class PropertyController(
    private val blockProcessor: BlockProcessor,
    private val propertyService: PropertyService
) {

    @ApiOperation(
        value = "Returns value of a property",
        notes = "Properties which were applied before the last irreversible block are called finalized. " +
            "Property can change it's value until finalized.",
        nickname = "getProperty",
        response = PropertyValue::class
    )
    @PostMapping("/search")
    fun getProperty(@RequestBody request: PropertyRequest): Value {
        return if (request.finalized) {
            propertyService.getPropertyValue(request.space, request.self, request.target, request.key)
        } else {
            blockProcessor.getLastPropertyValue(request.space, request.self, request.target, request.key)
        } ?: throw NotFoundException()
    }
}