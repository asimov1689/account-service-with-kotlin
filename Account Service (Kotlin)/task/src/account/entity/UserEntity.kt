package account.entity

import account.dto.UserAdminResponse
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "users")
data class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val lastname: String = "",

    @Column(unique = true, nullable = false)
    val email: String = "",

    @Column(nullable = false)
    var password: String = "",

    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.MERGE])
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: MutableSet<RoleEntity> = mutableSetOf(),

    @Column(nullable = false)
    var locked: Boolean = false,

    @Column(nullable = false)
    var failedLoginAttempts: Int = 0
)

fun UserEntity.sortedRoleNames(): List<String> =
    roles.map { it.name }.sorted()

fun UserEntity.toAdminResponse(): UserAdminResponse =
    UserAdminResponse(
        id = id,
        name = name,
        lastname = lastname,
        email = email,
        roles = sortedRoleNames()
    )