package org.bloqly.machine.test

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class TestConfiguration {

    @Bean
    fun getRestTemplate(): RestTemplate {
        return Mockito.mock(RestTemplate::class.java)
    }
}