package account.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["employee", "period"])]
)
data class PaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,

    val employee: String,

    val period: String,

    val salary: Long
)