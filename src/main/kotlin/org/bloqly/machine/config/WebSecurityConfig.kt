package org.bloqly.machine.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    // TODO add simple auth
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/v1/data/**")
            .permitAll()
            .antMatchers("/api/v1/admin/**")
            .access("hasIpAddress('127.0.0.1') or hasIpAddress('0:0:0:0:0:0:0:1')")
    }
}
