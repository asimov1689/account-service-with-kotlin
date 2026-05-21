package account.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["employee", "period"])],
)
data class PaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val employee: String,
    val period: String,
    val salary: Long,
)
