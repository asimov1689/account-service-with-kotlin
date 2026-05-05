package account.service

import account.dto.PaymentRequest
import account.entity.PaymentEntity
import account.exception.PaymentValidationException
import account.repository.PaymentRepository
import account.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository
) {

    companion object {
        private val PERIOD_REGEX = Regex("^(0[1-9]|1[0-2])-\\d{4}$")
    }

    // ─── VALIDATION HELPERS ───────────────────────────────────────────

    fun isValidPeriod(period: String): Boolean = period.matches(PERIOD_REGEX)

    fun formatSalary(cents: Long): String {
        val dollars = cents / 100
        val remainingCents = cents % 100
        return "$dollars dollar(s) $remainingCents cent(s)"
    }

    fun formatPeriod(period: String): String {
        val month = period.substring(0, 2).toInt()
        val year = period.substring(3)
        val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$monthName-$year"
    }

    // ─── POST — BATCH UPLOAD ──────────────────────────────────────────

    @Transactional
    fun addPayments(payments: List<PaymentRequest>) {
        // Phase 1: field-level validation — collect indexed errors
        val fieldErrors = mutableListOf<String>()
        payments.forEachIndexed { i, p ->
            if (p.salary == null || p.salary < 0) {
                fieldErrors.add("payments[$i].salary: Salary must be non negative!")
            }
            if (p.period == null || !isValidPeriod(p.period)) {
                fieldErrors.add("payments[$i].period: Wrong date!")
            }
        }
        if (fieldErrors.isNotEmpty()) {
            throw PaymentValidationException(fieldErrors.joinToString(", "))
        }

        // Phase 2: within-batch duplicate check (same employee+period in the same request)
        val batchKeys = payments.map { "${it.employee!!.lowercase()}_${it.period}" }
        if (batchKeys.size != batchKeys.toSet().size) {
            throw PaymentValidationException("Duplicate employee-period pair in request!")
        }

        // Phase 3: business rule validation — employee exists, DB uniqueness
        payments.forEach { p ->
            val email = p.employee!!.lowercase()
            if (userRepository.findByEmail(email) == null) {
                throw PaymentValidationException("Employee $email not found!")
            }
            if (paymentRepository.findByEmployeeAndPeriod(email, p.period!!) != null) {
                throw PaymentValidationException(
                    "Payment for $email in period ${p.period} already exists!"
                )
            }
        }

        // All checks passed — save all atomically
        val entities = payments.map { p ->
            PaymentEntity(
                employee = p.employee!!.lowercase(),
                period = p.period!!,
                salary = p.salary!!
            )
        }
        paymentRepository.saveAll(entities)
    }

    // ─── PUT — SINGLE SALARY UPDATE ───────────────────────────────────

    fun updatePayment(request: PaymentRequest) {
        if (request.salary == null || request.salary < 0) {
            throw PaymentValidationException("Salary must be non negative!")
        }
        if (request.period == null || !isValidPeriod(request.period)) {
            throw PaymentValidationException("Wrong date!")
        }
        val email = request.employee?.lowercase()
            ?: throw PaymentValidationException("Employee must not be blank!")

        if (userRepository.findByEmail(email) == null) {
            throw PaymentValidationException("Employee $email not found!")
        }

        val existing = paymentRepository.findByEmployeeAndPeriod(email, request.period)
            ?: throw PaymentValidationException(
                "No payment found for $email in period ${request.period}!"
            )

        paymentRepository.save(existing.copy(salary = request.salary))
    }

    // ─── GET — READ SALARY ─────────────────────────────────────────────

    fun getPayment(email: String, period: String): PaymentEntity? {
        return paymentRepository.findByEmployeeAndPeriod(email.lowercase(), period)
    }

    fun getAllPayments(email: String): List<PaymentEntity> {
        return paymentRepository.findByEmployee(email.lowercase())
            .sortedWith(
                compareByDescending<PaymentEntity> { it.period.substring(3, 7).toInt() }
                    .thenByDescending { it.period.substring(0, 2).toInt() }
            )
    }
}