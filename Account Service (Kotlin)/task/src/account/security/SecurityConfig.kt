package account.security

import account.exception.RestAccessDeniedHandler
import account.exception.RestAuthenticationEntryPoint
import account.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val restAccessDeniedHandler: RestAccessDeniedHandler
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(13)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        userService: UserService
    ): SecurityFilterChain =
        http
            .httpBasic(Customizer.withDefaults())
            .exceptionHandling {
                it.authenticationEntryPoint(restAuthenticationEntryPoint)
                it.accessDeniedHandler(restAccessDeniedHandler)
            }
            .csrf { it.disable() }
            .headers { it.frameOptions { fo -> fo.disable() } }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/signup").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/changepass").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/empl/payment")
                    .hasAnyAuthority("ROLE_USER", "ROLE_ACCOUNTANT")
                    .requestMatchers(HttpMethod.POST, "/api/acct/payments")
                    .hasAuthority("ROLE_ACCOUNTANT")
                    .requestMatchers(HttpMethod.PUT, "/api/acct/payments")
                    .hasAuthority("ROLE_ACCOUNTANT")
                    .requestMatchers("/api/admin/**")
                    .hasAuthority("ROLE_ADMINISTRATOR")
                    .anyRequest().authenticated()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .userDetailsService(userService)
            .build()
}