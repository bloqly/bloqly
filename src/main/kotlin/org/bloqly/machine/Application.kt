package org.bloqly.machine

import org.bloqly.machine.util.ApplicationUtils
import org.bloqly.machine.util.OptionUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.Properties

@EnableScheduling
@EnableJpaRepositories("org.bloqly.machine.repository")
@SpringBootApplication(scanBasePackages = ["org.bloqly.machine"])
class Application {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            System.err.close()
            System.setErr(System.out)

            val properties = Properties()

            val commandLine = OptionUtils.parseCommandLine(args)

            val database = commandLine.getOptionValue("database", "bloqly_main")

            properties["spring.datasource.url"] = "jdbc:postgresql:$database"

            if (commandLine.hasOption("server")) {
                ApplicationUtils.startServer(properties, commandLine)
            } else if (commandLine.hasOption("console") || commandLine.hasOption("command")) {
                ApplicationUtils.startConsole(properties, commandLine)
            }
        }

        const val DEFAULT_PORT = "9900"

        const val POWER_KEY = "power"
        const val GENESIS_KEY = "genesis"
        const val DEFAULT_SPACE = "main"
        const val DEFAULT_SELF = "self"
        const val QUORUM_KEY = "quorum"
        const val DEFAULT_FUNCTION_NAME = "contract"
        const val MAX_DELTA_SIZE = 1000
        // TODO what should it be?
        const val MAX_REFERENCED_BLOCK_DEPTH = 10
    }
}
