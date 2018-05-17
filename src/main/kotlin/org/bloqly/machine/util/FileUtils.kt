package org.bloqly.machine.util

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets.UTF_8

object FileUtils {

    fun getResourceAsString(path: String): String {
        return IOUtils.toString(FileUtils::class.java.getResourceAsStream(path), UTF_8.name())
    }
}
