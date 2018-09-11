package org.bloqly.machine.component

import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ObjectFilterService {

    private val maxElements = 1000000L

    private val elements = CacheBuilder.newBuilder()
        .maximumSize(maxElements)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Boolean>()

    fun add(key: String) {
        elements.put(key, true)
    }

    fun contains(key: String): Boolean = elements.getIfPresent(key) ?: false

    fun clear() {
        elements.invalidateAll()
    }
}