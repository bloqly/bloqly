package org.bloqly.machine.component

import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

@Component
class ObjectFilterService {

    private val expectedInsertions = 1000000

    private val probability = 0.01

    private val filter = AtomicReference(createFilter())

    private fun createFilter(): BloomFilter<String> =
        BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            expectedInsertions,
            probability
        )

    fun add(key: String) {
        if (filter.get().approximateElementCount() > expectedInsertions) {
            filter.set(createFilter())
        }
        filter.get().put(key)
    }

    fun mightContain(key: String): Boolean = filter.get().mightContain(key)
}