package account.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

data class SignupRequest(

    @field:NotBlank(message = "Name must not be blank")
    val name: String?,

    @field:NotBlank(message = "Last name must not be blank")
    val lastname: String?,

    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email format is invalid")
    @field:Pattern(
        regexp = "^[^@]+@acme\\.com$",
        message = "Email domain must be @acme.com"
    )
    val email: String?,

    @field:NotBlank(message = "Password must not be blank")
    val password: String?
)