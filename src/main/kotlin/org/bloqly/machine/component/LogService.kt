package org.bloqly.machine.component

import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class LogService(
    private val spaceRepository: SpaceRepository,
    private val accountService: AccountService
) {

    private val log = LoggerFactory.getLogger(LogService::class.simpleName)

    @PostConstruct
    fun init() {
        spaceRepository.findAll().forEach { space ->

            val validators = accountService.getValidatorsForSpace(space.id)

            if (validators.isNotEmpty()) {
                log.info("validators for space $space:")
                validators.forEach { account ->
                    log.info("\t${account.id}")
                }
            }
        }
    }
}