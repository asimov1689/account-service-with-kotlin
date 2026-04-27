package account.service

import account.dto.SignupRequest
import account.entity.UserEntity
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

    fun registerUser(request: SignupRequest): UserEntity {
        val email = request.email!!.lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw UserExistException()
        }
        return userRepository.save(
            UserEntity(
                name = request.name!!,
                lastname = request.lastname!!,
                email = email,
                password = passwordEncoder.encode(request.password!!)
            )
        )
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