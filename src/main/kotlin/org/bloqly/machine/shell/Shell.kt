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

        val accountServiceShell = context.getBean(AccountServiceShell::class.java)
        val blockServiceShell = context.getBean(BlockServiceShell::class.java)
        val transactionServiceShell = context.getBean(TransactionServiceShell::class.java)
        val chainServiceShell = context.getBean(ChainServiceShell::class.java)

        val binding = Binding()

        binding.setProperty("account", accountServiceShell)
        binding.setProperty("block", blockServiceShell)
        binding.setProperty("chain", chainServiceShell)
        binding.setProperty("txs", transactionServiceShell)

        val shell = Groovysh(binding, IO())

        if (commandLine.hasOption("command")) {
            try {
                val command = commandLine.getOptionValue("command")
                shell.execute(command)
            } catch (e: Exception) {
                log.error("Error executing command", e)
            }
        } else {
            shell.run(null)
        }
    }
}