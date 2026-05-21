package account.service

import account.dto.ChangePassResponse
import account.dto.SignupRequest
import account.dto.SignupResponse
import account.entity.RoleEntity
import account.entity.UserEntity
import account.entity.sortedRoleNames
import account.exception.BreachedPasswordException
import account.exception.PasswordTooShortException
import account.exception.PasswordsIdenticalException
import account.exception.UserExistException
import account.exception.UserNotFoundException
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
    private val passwordEncoder: PasswordEncoder,
    private val securityEventService: SecurityEventService,
) : UserDetailsService {
    companion object {
        private val BREACHED_PASSWORDS =
            setOf(
                "PasswordForJanuary",
                "PasswordForFebruary",
                "PasswordForMarch",
                "PasswordForApril",
                "PasswordForMay",
                "PasswordForJune",
                "PasswordForJuly",
                "PasswordForAugust",
                "PasswordForSeptember",
                "PasswordForOctober",
                "PasswordForNovember",
                "PasswordForDecember",
            )
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findByEmail(username.lowercase())
                ?: throw UsernameNotFoundException("User not found: $username")

        return User
            .builder()
            .username(user.email)
            .password(user.password)
            .authorities(user.roles.map { SimpleGrantedAuthority(it.name) })
            .accountLocked(user.locked)
            .build()
    }

    fun registerUser(request: SignupRequest): SignupResponse {
        val email = request.email!!.lowercase()

        if (userRepository.findByEmail(email) != null) {
            throw UserExistException()
        }

        val isFirstUser = userRepository.count() == 0L
        val roleName = if (isFirstUser) "ROLE_ADMINISTRATOR" else "ROLE_USER"
        val role = getOrCreateRole(roleName)

        val user =
            UserEntity(
                name = request.name!!,
                lastname = request.lastname!!,
                email = email,
                password = passwordEncoder.encode(request.password),
            )
        user.roles.add(role)

        val saved = userRepository.save(user)

        securityEventService.log(
            action = "CREATE_USER",
            subject = "Anonymous",
            obj = email,
            path = "/api/auth/signup",
        )

        return SignupResponse(
            id = saved.id,
            name = saved.name,
            lastname = saved.lastname,
            email = saved.email,
            roles = saved.sortedRoleNames(),
        )
    }

    fun changePassword(
        email: String,
        newPassword: String,
    ): ChangePassResponse {
        val user =
            userRepository.findByEmail(email.lowercase())
                ?: throw UserNotFoundException()

        validatePasswordSecurity(newPassword)

        if (passwordEncoder.matches(newPassword, user.password)) {
            throw PasswordsIdenticalException()
        }

        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)

        securityEventService.log(
            action = "CHANGE_PASSWORD",
            subject = email,
            obj = email,
            path = "/api/auth/changepass",
        )

        return ChangePassResponse(
            email = email,
            status = "The password has been updated successfully",
        )
    }

    fun incrementFailedLoginAttempts(
        email: String,
        path: String,
    ): Boolean {
        val user = userRepository.findByEmail(email.lowercase()) ?: return false
        if (user.locked) return false
        if (user.roles.any { it.name == "ROLE_ADMINISTRATOR" }) return false

        user.failedLoginAttempts++

        return if (user.failedLoginAttempts >= 5) {
            user.locked = true
            user.failedLoginAttempts = 0
            userRepository.save(user)
            true
        } else {
            userRepository.save(user)
            false
        }
    }

    fun resetFailedLoginAttempts(email: String) {
        val user = userRepository.findByEmail(email.lowercase()) ?: return
        if (user.failedLoginAttempts == 0) return
        user.failedLoginAttempts = 0
        userRepository.save(user)
    }

    fun lockUser(email: String) {
        val user =
            userRepository.findByEmail(email.lowercase())
                ?: throw UserNotFoundException()
        user.locked = true
        userRepository.save(user)
    }

    fun unlockUser(email: String) {
        val user =
            userRepository.findByEmail(email.lowercase())
                ?: throw UserNotFoundException()
        user.locked = false
        user.failedLoginAttempts = 0
        userRepository.save(user)
    }

    fun validatePasswordSecurity(password: String) {
        if (password.length < 12) {
            throw PasswordTooShortException()
        }
        if (password in BREACHED_PASSWORDS) {
            throw BreachedPasswordException()
        }
    }

    fun getOrCreateRole(name: String): RoleEntity = roleRepository.findByName(name) ?: roleRepository.save(RoleEntity(name = name))
}
