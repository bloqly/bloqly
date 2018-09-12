package org.bloqly.machine

import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.test.TestUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

// TODO remove this class from production classpath
@Profile("test-node")
@EnableScheduling
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableJpaRepositories("org.bloqly.machine.repository")
@SpringBootApplication(
    scanBasePackages = ["org.bloqly.machine"],
    exclude = [ElasticsearchAutoConfiguration::class]
)
class TestApplication(
    private val testService: TestService,
    private val blockchainService: BlockchainService,
    private val passphraseService: PassphraseService
) {

    private val log = LoggerFactory.getLogger(TestApplication::class.simpleName)

    @EventListener(ApplicationReadyEvent::class)
    fun ontTestServerStart() {
        log.info("Cleaning up test database.")
        testService.cleanup()

        log.info("Creating test blockchain.")
        testService.importAccounts()

        blockchainService.createBlockchain(
            Application.DEFAULT_SPACE,
            TestUtils.TEST_BLOCK_SINGLE_BASE_DIR,
            passphraseService.getPassphrase(testService.getRoot().accountId)
        )

        log.info("Test server prepared.")
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder()
                .profiles("server", "scheduler")
                .sources(TestApplication::class.java)
                .build()
                .run()
        }
    }
}