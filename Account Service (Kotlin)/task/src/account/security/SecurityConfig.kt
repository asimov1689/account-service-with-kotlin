package account.security

import account.security.RestAccessDeniedHandler
import account.exception.RestAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationEntryPoint: RestAuthenticationEntryPoint,
    private val accessDeniedHandler: RestAccessDeniedHandler,
    private val loginAuditFilter: LoginAuditFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .httpBasic { it.authenticationEntryPoint(authenticationEntryPoint) }
            .exceptionHandling { it.accessDeniedHandler(accessDeniedHandler) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .headers { it.frameOptions { fo -> fo.disable() } }   // H2 console if needed
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/changepass").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/empl/payment").hasAnyRole("USER", "ACCOUNTANT")
                    .requestMatchers(HttpMethod.POST, "/api/acct/payments").hasRole("ACCOUNTANT")
                    .requestMatchers(HttpMethod.PUT, "/api/acct/payments").hasRole("ACCOUNTANT")
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                    .requestMatchers(HttpMethod.PUT, "/api/admin/user/access").hasRole("ADMINISTRATOR")
                    .requestMatchers(HttpMethod.GET, "/api/security/events").hasRole("AUDITOR")
                    .anyRequest().authenticated()
            }

            .addFilterBefore(loginAuditFilter, BasicAuthenticationFilter::class.java)

        return http.build()
    }

}