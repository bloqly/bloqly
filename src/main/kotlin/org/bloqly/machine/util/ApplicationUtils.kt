package org.bloqly.machine.util

import org.apache.commons.cli.CommandLine
import org.bloqly.machine.Application
import org.bloqly.machine.shell.Shell
import org.slf4j.LoggerFactory
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

object ApplicationUtils {

    private val log = LoggerFactory.getLogger(ApplicationUtils::class.simpleName)

    fun startServer(properties: Properties, commandLine: CommandLine) {

        println("Starting server.")

        //System.err.close()

        properties["server.port"] = commandLine.getOptionValue("port", Application.DEFAULT_PORT)
        properties["nodes"] = commandLine.getOptionValue("nodes", "")

        if (commandLine.hasOption("validators")) {
            properties["validators"] = commandLine.getOptionValue("validators")
        }

        val appBuilder = SpringApplicationBuilder()
                .profiles("server")
                .properties(properties)
                .bannerMode(Banner.Mode.OFF)
                .sources(Application::class.java)

        appBuilder
                .build()
                .run()
    }

    fun startConsole(properties: Properties, commandLine: CommandLine) {

        println("Starting console.")

        //System.err.close()

        val context = SpringApplicationBuilder()
                .logStartupInfo(false)
                .properties(properties)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.NONE)
                .sources(Application::class.java)
                .build()
                .run()

        Shell.run(context)
    }
}