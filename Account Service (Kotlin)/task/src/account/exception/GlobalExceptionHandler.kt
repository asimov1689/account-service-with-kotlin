package account.exception

import account.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private fun errorResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> = ResponseEntity(
        ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        ),
        status
    )

    @ExceptionHandler(UserExistException::class)
    fun handleUserExist(
        ex: UserExistException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, "User exist!", request)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(
        ex: UserNotFoundException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.NOT_FOUND, ex.message!!, request)

    @ExceptionHandler(AdminOperationException::class)
    fun handleAdminOperation(
        ex: AdminOperationException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, ex.message!!, request)

    @ExceptionHandler(PasswordTooShortException::class)
    fun handlePasswordTooShort(
        ex: PasswordTooShortException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, ex.message!!, request)

    @ExceptionHandler(BreachedPasswordException::class)
    fun handleBreachedPassword(
        ex: BreachedPasswordException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, ex.message!!, request)

    @ExceptionHandler(PasswordsIdenticalException::class)
    fun handlePasswordsIdentical(
        ex: PasswordsIdenticalException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, ex.message!!, request)

    @ExceptionHandler(PaymentValidationException::class)
    fun handlePaymentValidation(
        ex: PaymentValidationException,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.BAD_REQUEST, ex.message!!, request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ) = errorResponse(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Method ${ex.method} is not supported for this endpoint",
        request
    )

    @ExceptionHandler(Exception::class)
    fun handleGenericError(
        ex: Exception,
        request: HttpServletRequest
    ) = errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request)
}