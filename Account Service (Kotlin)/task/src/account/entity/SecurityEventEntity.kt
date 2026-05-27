package account.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "security_events")
data class SecurityEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val date: LocalDate = LocalDate.now(),
    @Column(nullable = false)
    val action: String = "",
    @Column(nullable = false)
    val subject: String = "",
    @Column(name = "object_field", nullable = false)
    val `object`: String = "",
    @Column(nullable = false)
    val path: String = "",
)
