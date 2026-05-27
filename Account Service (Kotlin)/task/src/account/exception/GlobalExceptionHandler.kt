package account.exception

import account.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.LockedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UserExistException::class)
    fun handleUserExist(
        ex: UserExistException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "User exist!", request)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(
        ex: UserNotFoundException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.message ?: "User not found!", request)

    @ExceptionHandler(AdminOperationException::class)
    fun handleAdminOperation(
        ex: AdminOperationException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Bad request", request)

    @ExceptionHandler(PasswordTooShortException::class)
    fun handlePasswordTooShort(
        ex: PasswordTooShortException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Password too short", request)

    @ExceptionHandler(BreachedPasswordException::class)
    fun handleBreachedPassword(
        ex: BreachedPasswordException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Password breached", request)

    @ExceptionHandler(PasswordsIdenticalException::class)
    fun handlePasswordsIdentical(
        ex: PasswordsIdenticalException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Passwords identical", request)

    @ExceptionHandler(PaymentValidationException::class)
    fun handlePaymentValidation(
        ex: PaymentValidationException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Payment validation error", request)

    @ExceptionHandler(LockedException::class)
    fun handleLocked(
        ex: LockedException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "User account is locked", request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.message ?: "Method not allowed", request)

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest,
    ) = errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request)

    private fun errorResponse(
        status: HttpStatus,
        error: String,
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val body =
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = status.value(),
                error = error,
                message = message,
                path = request.requestURI,
            )
        return ResponseEntity.status(status).body(body)
    }
}
