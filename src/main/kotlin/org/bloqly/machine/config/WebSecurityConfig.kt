package org.bloqly.machine.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    companion object {
        private const val LOCALHOST = "127.0.0.1"
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers("/api/v1/data/**")
    }

    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .antMatchers("/api/v1/admin/**").hasIpAddress(LOCALHOST)
    }
}