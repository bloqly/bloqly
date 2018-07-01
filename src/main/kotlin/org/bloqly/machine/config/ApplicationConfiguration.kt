package org.bloqly.machine.config

import org.bloqly.machine.Application
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ApplicationConfiguration {

    @Bean
    fun getRestTemplate(): RestTemplate {

        return RestTemplate()
    }

    @Bean("requestExecutor")
    fun getRequestExecutorThreadPool(): ExecutorService {
        return Executors.newFixedThreadPool(Application.REQUEST_THREADS)
    }
}