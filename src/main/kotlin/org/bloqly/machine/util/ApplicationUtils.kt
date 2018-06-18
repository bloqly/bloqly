package org.bloqly.machine.util

import org.apache.commons.cli.CommandLine
import org.bloqly.machine.Application
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

object ApplicationUtils {

    fun startServer(commandLine: CommandLine) {

        val properties = Properties()

        val database = commandLine.getOptionValue("database", "bloqly_main")

        properties["spring.datasource.url"] = "jdbc:postgresql:$database"

        properties["server.port"] = commandLine.getOptionValue("port", Application.DEFAULT_PORT)
        properties["nodes"] = commandLine.getOptionValue("nodes", "")

        val appBuilder = SpringApplicationBuilder()
            .profiles("server", "scheduler")
            .properties(properties)
            .bannerMode(Banner.Mode.OFF)
            .sources(Application::class.java)

        appBuilder
            .build()
            .run()
    }
}