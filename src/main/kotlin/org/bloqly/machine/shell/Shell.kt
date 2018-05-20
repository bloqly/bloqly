package org.bloqly.machine.shell

import groovy.lang.Binding
import org.apache.commons.cli.CommandLine
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

object Shell {

    private val log = LoggerFactory.getLogger(Shell::class.simpleName)

    fun run(context: ApplicationContext, commandLine: CommandLine) {

        val accountServiceShell = context.getBean("accountServiceShell")
        val blockServiceShell = context.getBean("blockServiceShell")

        val binding = Binding()

        binding.setProperty("account", accountServiceShell)
        binding.setProperty("bloq", blockServiceShell)

        val shell = Groovysh(binding, IO())

        if (commandLine.hasOption("command")) {
            try {
                val command = commandLine.getOptionValue("command")
                println("command: $command")

                shell.execute(command)
                println("OK")
            } catch (e: Exception) {
                log.error("Error executing command", e)
            }

        } else {
            shell.run(null)
        }
    }
}