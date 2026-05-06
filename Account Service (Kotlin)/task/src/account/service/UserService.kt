package account.service

import account.dto.SignupRequest
import account.entity.RoleEntity
import account.entity.UserEntity
import account.exception.BreachedPasswordException
import account.exception.PasswordTooShortException
import account.exception.PasswordsIdenticalException
import account.exception.UserExistException
import account.repository.RoleRepository
import account.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 12
        private val BREACHED_PASSWORDS = setOf(
            "PasswordForJanuary", "PasswordForFebruary", "PasswordForMarch", "PasswordForApril",
            "PasswordForMay", "PasswordForJune", "PasswordForJuly", "PasswordForAugust",
            "PasswordForSeptember", "PasswordForOctober", "PasswordForNovember", "PasswordForDecember"
        )
    }

    fun validatePasswordSecurity(password: String) {
        if (password.length < MIN_PASSWORD_LENGTH) throw PasswordTooShortException()
        if (password in BREACHED_PASSWORDS) throw BreachedPasswordException()
    }

    fun getOrCreateRole(name: String): RoleEntity =
        roleRepository.findByName(name) ?: roleRepository.save(RoleEntity(name = name))

    fun registerUser(request: SignupRequest): UserEntity {
        val email = request.email!!.lowercase()
        if (userRepository.findByEmail(email) != null) throw UserExistException()
        validatePasswordSecurity(request.password!!)

        val roleName = if (userRepository.count() == 0L) "ROLE_ADMINISTRATOR" else "ROLE_USER"
        val role = getOrCreateRole(roleName)

        return userRepository.save(
            UserEntity(
                name = request.name!!,
                lastname = request.lastname!!,
                email = email,
                password = passwordEncoder.encode(request.password),
                roles = mutableSetOf(role)
            )
        )
    }

    fun changePassword(email: String, newPassword: String): UserEntity {
        validatePasswordSecurity(newPassword)
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found")
        if (passwordEncoder.matches(newPassword, user.password)) {
            throw PasswordsIdenticalException()
        }
        return userRepository.save(user.copy(password = passwordEncoder.encode(newPassword)))
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username.lowercase())
            ?: throw UsernameNotFoundException("User not found")

        val authorities = user.roles.map { SimpleGrantedAuthority(it.name) }

        return User(user.email, user.password, authorities)
    }
}