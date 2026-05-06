// task/src/account/controller/PaymentController.kt
package account.controller

import account.dto.PaymentResponse
import account.exception.PaymentValidationException
import account.repository.UserRepository
import account.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/empl")
class PaymentController(
    private val paymentService: PaymentService,
    private val userRepository: UserRepository
) {

    @GetMapping("/payment")
    fun getPayment(
        @RequestParam(required = false) period: String?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Any> {
        val email = userDetails.username

        return if (period != null) {
            // Validate period format
            if (!paymentService.isValidPeriod(period)) {
                throw PaymentValidationException("Wrong date!")
            }

            val payment = paymentService.getPayment(email, period)
            if (payment == null) {
                ResponseEntity.ok(emptyMap<String, Any>())
            } else {
                val user = userRepository.findByEmail(email)!!
                ResponseEntity.ok(
                    PaymentResponse(
                        name = user.name,
                        lastname = user.lastname,
                        period = paymentService.formatPeriod(payment.period),
                        salary = paymentService.formatSalary(payment.salary)
                    )
                )
            }
        } else {
            val payments = paymentService.getAllPayments(email)
            if (payments.isEmpty()) {
                ResponseEntity.ok(emptyList<PaymentResponse>())
            } else {
                val user = userRepository.findByEmail(email)!!
                ResponseEntity.ok(
                    payments.map { p ->
                        PaymentResponse(
                            name = user.name,
                            lastname = user.lastname,
                            period = paymentService.formatPeriod(p.period),
                            salary = paymentService.formatSalary(p.salary)
                        )
                    }
                )
            }
        }
    }
}