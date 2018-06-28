package org.bloqly.machine.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class ApplicationConfiguration {

    @Bean
    fun getRestTemplate(): RestTemplate {

        return RestTemplate()
    }
}