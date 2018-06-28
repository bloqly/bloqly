package org.bloqly.machine.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper

object ObjectUtils {

    private val mapper = ObjectMapper()

    init {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun <T> readValue(content: String, valueType: Class<T>): T {
        return mapper.readValue(content, valueType)
    }

    fun <T> readValue(content: ByteArray, valueType: Class<T>): T {
        return mapper.readValue(content, valueType)
    }

    fun writeValueAsString(value: Any): String {
        return mapper.writeValueAsString(value)
    }
}