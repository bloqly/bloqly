package org.bloqly.machine.controller.data

import org.bloqly.machine.model.PropertyValue
import org.bloqly.machine.repository.PropertyService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/data/properties")
class PropertyController(
    private val propertyService: PropertyService
) {

    @GetMapping
    fun getProperty(
        @RequestParam("space") spaceId: String,
        @RequestParam("self") self: String,
        @RequestParam("target") target: String,
        @RequestParam("key") key: String
    ): PropertyValue {
        return propertyService.getProperty(spaceId, self, target, key)
    }
}