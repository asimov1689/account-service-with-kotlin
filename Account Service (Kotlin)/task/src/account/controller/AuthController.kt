package account.controller

import account.dto.SignupRequest
import account.dto.SignupResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController {

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest
    ): ResponseEntity<SignupResponse> {

        val response = SignupResponse(
            name = request.name,
            lastname = request.lastname,
            email = request.email
        )

        return ResponseEntity.ok(response)
    }
}