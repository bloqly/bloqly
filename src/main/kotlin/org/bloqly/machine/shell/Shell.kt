package org.bloqly.machine.shell

import groovy.lang.Binding
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO
import org.springframework.context.ApplicationContext

object Shell {

    fun run(context: ApplicationContext) {

        val accountServiceShell = context.getBean("accountServiceShell")
        val blockServiceShell = context.getBean("blockServiceShell")

        val binding = Binding()

        binding.setProperty("account", accountServiceShell)
        binding.setProperty("bloq", blockServiceShell)

        val shell = Groovysh(binding, IO())

        shell.run(null)
    }
}