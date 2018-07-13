package org.bloqly.machine

import org.bloqly.machine.util.ApplicationUtils
import org.bloqly.machine.util.OptionUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableJpaRepositories("org.bloqly.machine.repository")
@SpringBootApplication(
    scanBasePackages = ["org.bloqly.machine"],
    exclude = [ElasticsearchAutoConfiguration::class]
)
class Application {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            ApplicationUtils.startServer(
                OptionUtils.parseCommandLine(args)
            )
        }

        const val REQUEST_THREADS = 5
        const val DEFAULT_PORT = "9900"
        const val POWER_KEY = "power"
        const val DEFAULT_SPACE = "main"
        const val DEFAULT_SELF = "self"
        const val QUORUM_KEY = "quorum"
        const val DEFAULT_KEY = "contract"
        const val INIT_KEY = "init"
        const val MAX_DELTA_SIZE = 1000
        const val MAX_REFERENCED_BLOCK_DEPTH = 720
        const val MAX_TRANSACTION_AGE = 3600 * 1000
        const val ROUND = 5000
    }
}
