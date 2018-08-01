package org.bloqly.machine.config

import org.bloqly.machine.Application
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ApplicationConfiguration(
    @Value("\${admin.port}") private val adminPort: Int
) {

    @Bean
    fun getRestTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean("requestExecutor")
    fun getRequestExecutorThreadPool(): ExecutorService {
        return Executors.newFixedThreadPool(Application.REQUEST_THREADS)
    }

    @Bean
    fun servletWebServerFactory(): UndertowServletWebServerFactory {
        val factory = UndertowServletWebServerFactory()

        factory.addBuilderCustomizers(UndertowBuilderCustomizer { builder ->
            builder.addHttpListener(adminPort, "0.0.0.0")
        })

        return factory
    }
}