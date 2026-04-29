package account.service

import account.dto.SignupRequest
import account.entity.UserEntity
import account.exception.BreachedPasswordException
import account.exception.PasswordTooShortException
import account.exception.PasswordsIdenticalException
import account.exception.UserExistException
import account.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
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
        if (BREACHED_PASSWORDS.contains(password)) throw BreachedPasswordException()
    }

    fun registerUser(request: SignupRequest): UserEntity {
        val email = request.email!!.lowercase()
        if (userRepository.findByEmail(email) != null) throw UserExistException()

        validatePasswordSecurity(request.password!!)

        return userRepository.save(
            UserEntity(
                name = request.name!!,
                lastname = request.lastname!!,
                email = email,
                password = passwordEncoder.encode(request.password)
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

        val updated = user.copy(password = passwordEncoder.encode(newPassword))
        return userRepository.save(updated)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username.lowercase())
            ?: throw UsernameNotFoundException("User not found")
        return User.withUsername(user.email)
            .password(user.password)
            .roles("USER")
            .build()
    }
}