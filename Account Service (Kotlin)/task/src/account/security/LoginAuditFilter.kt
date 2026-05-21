package account.security

import account.service.SecurityEventService
import account.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

@Component
class LoginAuditFilter(
    private val userService: UserService,
    private val securityEventService: SecurityEventService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        chain.doFilter(request, response)

        val path = request.requestURI
        val status = response.status

        if (path == "/api/auth/signup") return

        when {
            status == 401 -> {
                val attemptedEmail = extractEmailFromAuthHeader(request) ?: "Anonymous"

                securityEventService.log(
                    action = "LOGIN_FAILED",
                    subject = attemptedEmail,
                    obj = path,
                    path = path,
                )

                val justLocked = userService.incrementFailedLoginAttempts(attemptedEmail, path)
                if (justLocked) {
                    securityEventService.log(
                        action = "BRUTE_FORCE",
                        subject = attemptedEmail,
                        obj = path,
                        path = path,
                    )
                    securityEventService.log(
                        action = "LOCK_USER",
                        subject = attemptedEmail,
                        obj = "Lock user $attemptedEmail",
                        path = path,
                    )
                }
            }

            status in 200..299 -> {
                val authenticatedEmail =
                    SecurityContextHolder
                        .getContext()
                        .authentication
                        ?.name
                if (authenticatedEmail != null) {
                    userService.resetFailedLoginAttempts(authenticatedEmail)
                }
            }
        }
    }

    private fun extractEmailFromAuthHeader(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Basic ")) return null

        return try {
            val decoded = String(Base64.getDecoder().decode(header.removePrefix("Basic ")))
            decoded.substringBefore(":")
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
