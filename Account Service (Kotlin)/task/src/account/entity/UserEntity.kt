package account.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,

    val name: String,

    val lastname: String,

    @Column(unique = true)
    val email: String,

    val password: String
)