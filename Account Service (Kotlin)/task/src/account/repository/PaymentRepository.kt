package account.repository

import account.entity.PaymentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<PaymentEntity, Long> {
    fun findByEmployee(email: String): List<PaymentEntity>
    fun findByEmployeeAndPeriod(email: String, period: String): PaymentEntity?
}