package account.controller

import account.dto.ChangePassRequest
import account.dto.ChangePassResponse
import account.dto.ErrorResponse
import account.dto.SignupRequest
import account.dto.SignupResponse
import account.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userService: UserService) {

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

        val user = userService.registerUser(request)

        return ResponseEntity.ok(
            SignupResponse(
                id = user.id,
                name = user.name,
                lastname = user.lastname,
                email = user.email
            )
        )
    }

    @PostMapping("/changepass")
    fun changePassword(
        @RequestBody request: ChangePassRequest,
        @AuthenticationPrincipal userDetails: UserDetails,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Any> {

        if (request.new_password.isNullOrBlank()) {
            return ResponseEntity(
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = "Password must not be blank",
                    path = httpRequest.requestURI
                ),
                HttpStatus.BAD_REQUEST
            )
        }

        val updatedUser = userService.changePassword(
            email = userDetails.username,
            newPassword = request.new_password
        )

        return ResponseEntity.ok(
            ChangePassResponse(
                email = updatedUser.email,
                status = "The password has been updated successfully"
            )
        )
    }

    internal fun validate(request: SignupRequest): List<String> {
        val errors = mutableListOf<String>()
        if (request.name.isNullOrBlank()) errors.add("Name must not be blank")
        if (request.lastname.isNullOrBlank()) errors.add("Last name must not be blank")
        if (request.email.isNullOrBlank()) {
            errors.add("Email must not be blank")
        } else if (!request.email.matches(Regex("^[^@]+@acme\\.com$"))) {
            errors.add("Email domain must be @acme.com")
        }
        if (request.password.isNullOrBlank()) errors.add("Password must not be blank")
        return errors
    }
}