package org.bloqly.machine

import org.bloqly.machine.util.ApplicationUtils
import org.bloqly.machine.util.OptionUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.Properties

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("org.bloqly.machine.repository")
class Application {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val properties = Properties()

            val commandLine = OptionUtils.parseCommandLine(args)

            val database = commandLine.getOptionValue("database", "db")

            properties["spring.datasource.url"] = "jdbc:h2:~/.bloqly/$database;AUTO_SERVER=TRUE"

            if (commandLine.hasOption("server")) {

                ApplicationUtils.startServer(properties, commandLine)

            } else if (commandLine.hasOption("console") || commandLine.hasOption("command")) {

                ApplicationUtils.startConsole(properties, commandLine)

            }
        }

        const val DEFAULT_PORT = "9900"

        const val POWER_KEY = "power"
        const val DEFAULT_SPACE = "main"
        const val DEFAULT_SELF = "_self"
        const val QUORUM_KEY = "quorum"
        const val DEFAULT_FUNCTION_NAME = "contract"
    }
}
