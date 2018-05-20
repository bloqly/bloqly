package org.bloqly.machine.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate


@Configuration
class ApplicationConfiguration {

    @Bean
    fun getRestTemplate(): RestTemplate {

        return RestTemplate()
    }

    @Bean
    fun getObjectMapper(): ObjectMapper {

        val yamlFactory = YAMLFactory()

        yamlFactory.enable(YAMLGenerator.Feature.INDENT_ARRAYS)
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)

        return ObjectMapper(yamlFactory)
    }

    @Bean
    fun getObjectReader(): ObjectWriter {

        return getObjectMapper().writerWithDefaultPrettyPrinter()
    }
}