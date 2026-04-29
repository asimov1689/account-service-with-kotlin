package account.exception

import account.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserExistException::class)
    fun handleUserExist(
        ex: UserExistException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = "User exist!",
                path = request.requestURI
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val body = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.METHOD_NOT_ALLOWED.value(),
            error = "Method Not Allowed",
            message = "Method ${ex.method} is not supported for this endpoint",
            path = request.requestURI
        )

        return ResponseEntity(body, HttpStatus.METHOD_NOT_ALLOWED)
    }

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

    @ExceptionHandler(PasswordTooShortException::class)
    fun handlePasswordTooShort(
        ex: PasswordTooShortException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> = ResponseEntity(
        ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message!!,
            path = request.requestURI
        ),
        HttpStatus.BAD_REQUEST
    )

    @ExceptionHandler(BreachedPasswordException::class)
    fun handleBreachedPassword(
        ex: BreachedPasswordException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> = ResponseEntity(
        ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message!!,
            path = request.requestURI
        ),
        HttpStatus.BAD_REQUEST
    )

    @ExceptionHandler(PasswordsIdenticalException::class)
    fun handlePasswordsIdentical(
        ex: PasswordsIdenticalException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> = ResponseEntity(
        ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message!!,
            path = request.requestURI
        ),
        HttpStatus.BAD_REQUEST
    )
}