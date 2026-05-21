package account.controller

import account.dto.PaymentRequest
import account.dto.StatusResponse
import account.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/acct")
class AccountController(
    private val paymentService: PaymentService,
) {
    @PostMapping("/payments")
    fun addPayments(
        @RequestBody payments: List<PaymentRequest>,
    ): ResponseEntity<StatusResponse> {
        paymentService.addPayments(payments)
        return ResponseEntity.ok(StatusResponse("Added successfully!"))
    }

    @PutMapping("/payments")
    fun updatePayment(
        @RequestBody request: PaymentRequest,
    ): ResponseEntity<StatusResponse> {
        paymentService.updatePayment(request)
        return ResponseEntity.ok(StatusResponse("Updated successfully!"))
    }
}
