package account.security

import account.dto.ErrorResponse
import account.service.SecurityEventService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
    private val securityEventService: SecurityEventService
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val userEmail = SecurityContextHolder.getContext().authentication?.name ?: "Anonymous"

        securityEventService.log(
            action = "ACCESS_DENIED",
            subject = userEmail,
            obj = request.requestURI,
            path = request.requestURI
        )

        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val errorBody = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = 403,
            error = "Forbidden",
            message = "Access Denied!",
            path = request.requestURI
        )
        response.writer.write(objectMapper.writeValueAsString(errorBody))
    }
}