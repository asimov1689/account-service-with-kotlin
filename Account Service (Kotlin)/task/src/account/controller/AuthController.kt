package account.controller

import account.dto.ErrorResponse
import account.dto.SignupRequest
import account.dto.SignupResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @PostMapping("/signup")
    fun signup(
        @RequestBody request: SignupRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Any> {

        val errors = validate(request)
        if (errors.isNotEmpty()) {
            return ResponseEntity(
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = errors.joinToString(", "),
                    path = httpRequest.requestURI
                ),
                HttpStatus.BAD_REQUEST
            )
        }

        return ResponseEntity.ok(
            SignupResponse(
                name = request.name ?: "",
                lastname = request.lastname ?: "",
                email = request.email ?: ""
            )
        )
    }

    internal fun validate(request: SignupRequest): List<String> {
        val errors = mutableListOf<String>()

        if (request.name.isNullOrBlank())
            errors.add("Name must not be blank")

        if (request.lastname.isNullOrBlank())
            errors.add("Last name must not be blank")

        if (request.email.isNullOrBlank()) {
            errors.add("Email must not be blank")
        } else if (!request.email.matches(Regex("^[^@]+@acme\\.com$"))) {
            errors.add("Email domain must be @acme.com")
        }

        if (request.password.isNullOrBlank()) {
            errors.add("Password must not be blank")
        }

        return errors
    }
}