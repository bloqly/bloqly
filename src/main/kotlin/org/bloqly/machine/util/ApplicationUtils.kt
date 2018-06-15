package org.bloqly.machine.util

import org.apache.commons.cli.CommandLine
import org.bloqly.machine.Application
import org.bloqly.machine.shell.Shell
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

object ApplicationUtils {

    fun startServer(properties: Properties, commandLine: CommandLine) {

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

    fun startConsole(properties: Properties, commandLine: CommandLine) {

        val context = SpringApplicationBuilder()
            .logStartupInfo(false)
            .properties(properties)
            .bannerMode(Banner.Mode.OFF)
            .web(WebApplicationType.NONE)
            .sources(Application::class.java)
            .build()
            .run()

        Shell.run(context, commandLine)
    }
}