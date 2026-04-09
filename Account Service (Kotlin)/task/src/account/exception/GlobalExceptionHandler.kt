package account.exception

import account.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleGenericError(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val body = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.requestURI
        )

        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}