package account.controller

import account.dto.SignupResponse
import account.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/empl")
class PaymentController(private val userRepository: UserRepository) {

    @GetMapping("/payment/")
    fun getPayment(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<SignupResponse> {
        val user = userRepository.findByEmail(userDetails.username)
            ?: return ResponseEntity.status(401).build()

        return ResponseEntity.ok(
            SignupResponse(
                id = user.id,
                name = user.name,
                lastname = user.lastname,
                email = user.email
            )
        )
    }
}