package org.bloqly.machine.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    @Value("\${admin.username}") private val adminUsername: String,
    @Value("\${admin.password}") private val adminPassword: String
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Autowired
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth.inMemoryAuthentication()
            .passwordEncoder(passwordEncoder())
            .withUser(adminUsername)
            .password(passwordEncoder().encode(adminPassword))
            .roles("USER")
    }

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/v1/data/**")
            .permitAll()
            .antMatchers("/api/v1/admin/**")
            .access(
                "(hasIpAddress('127.0.0.1') or hasIpAddress('0:0:0:0:0:0:0:1')) and hasRole('USER')"
            )
            .and()
            .httpBasic()
            .and()
            .headers()
            .frameOptions()
            .sameOrigin()
    }
}
