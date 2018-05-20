package org.bloqly.machine.util

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options

object OptionUtils {

    fun parseCommandLine(args: Array<String>): CommandLine {

        val options = Options()

        options.addOption("console", false, "run in console mode")
        options.addOption("server", false, "run in server mode")
        options.addOption("port", true, "server port")
        options.addOption("database", true, "database name")
        options.addOption("nodes", true, "list of nodes")
        options.addOption("validators", false, "list of active validators")

        return DefaultParser().parse(options, args)
    }
}