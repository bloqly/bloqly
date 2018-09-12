package org.bloqly.machine.util

import org.bloqly.machine.Application
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

object ApplicationUtils {

    fun startServer() {
        val appBuilder = SpringApplicationBuilder()
            .profiles("server", "scheduler", "production")
            .bannerMode(Banner.Mode.OFF)
            .sources(Application::class.java)

        appBuilder
            .build()
            .run()
    }
}