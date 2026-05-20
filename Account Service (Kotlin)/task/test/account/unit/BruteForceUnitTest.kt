package account

import account.entity.RoleEntity
import account.entity.UserEntity
import account.repository.RoleRepository
import account.repository.UserRepository
import account.service.SecurityEventService
import account.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@ExtendWith(MockitoExtension::class)
class BruteForceUnitTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var roleRepository: RoleRepository
    @Mock private lateinit var securityEventService: SecurityEventService

    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        userService = UserService(
            userRepository,
            roleRepository,
            BCryptPasswordEncoder(4),
            securityEventService
        )
    }

    private fun userWithAttempts(attempts: Int, locked: Boolean = false): UserEntity =
        UserEntity(
            id = 1L,
            name = "Test",
            lastname = "User",
            email = "user@acme.com",
            password = "hashed",
            locked = locked,
            failedLoginAttempts = attempts,
            roles = mutableSetOf(RoleEntity(id = 1L, name = "ROLE_USER"))
        )

    @Test
    fun `given 4 failed attempts when increment called then returns false and does not lock`() {
        // Arrange
        val user = userWithAttempts(3)
        `when`(userRepository.findByEmail("user@acme.com")).thenReturn(user)
        `when`(userRepository.save(user)).thenReturn(user)

        // Act
        val locked = userService.incrementFailedLoginAttempts("user@acme.com", "/api/empl/payment")

        // Assert
        assertThat(locked).isFalse()
        assertThat(user.failedLoginAttempts).isEqualTo(4)
        assertThat(user.locked).isFalse()
    }

    @Test
    fun `given 4 failed attempts when 5th increment called then returns true and locks account`() {
        // Arrange
        val user = userWithAttempts(4)
        `when`(userRepository.findByEmail("user@acme.com")).thenReturn(user)
        `when`(userRepository.save(user)).thenReturn(user)

        // Act
        val locked = userService.incrementFailedLoginAttempts("user@acme.com", "/api/empl/payment")

        // Assert — the 5th attempt tips it over
        assertThat(locked).isTrue()
        assertThat(user.locked).isTrue()
        assertThat(user.failedLoginAttempts).isEqualTo(0)   // resets to 0 after lock
    }

    @Test
    fun `given already locked account when increment called then returns false and does not re-lock`() {
        // Arrange
        val user = userWithAttempts(0, locked = true)
        `when`(userRepository.findByEmail("user@acme.com")).thenReturn(user)

        // Act
        val locked = userService.incrementFailedLoginAttempts("user@acme.com", "/api/empl/payment")

        // Assert
        assertThat(locked).isFalse()
        verify(userRepository, never()).save(user)
    }

    @Test
    fun `given 3 failed attempts when reset called then sets counter to 0`() {
        // Arrange
        val user = userWithAttempts(3)
        `when`(userRepository.findByEmail("user@acme.com")).thenReturn(user)
        `when`(userRepository.save(user)).thenReturn(user)

        // Act
        userService.resetFailedLoginAttempts("user@acme.com")

        // Assert
        assertThat(user.failedLoginAttempts).isEqualTo(0)
        verify(userRepository).save(user)
    }

    @Test
    fun `given 0 failed attempts when reset called then skips unnecessary DB write`() {
        // Arrange
        val user = userWithAttempts(0)
        `when`(userRepository.findByEmail("user@acme.com")).thenReturn(user)

        // Act
        userService.resetFailedLoginAttempts("user@acme.com")

        // Assert
        verify(userRepository, never()).save(user)
    }

    @Test
    fun `given unknown email when increment called then returns false gracefully`() {
        // Arrange
        `when`(userRepository.findByEmail("ghost@acme.com")).thenReturn(null)

        // Act
        val result = userService.incrementFailedLoginAttempts("ghost@acme.com", "/api/empl/payment")

        // Assert
        assertThat(result).isFalse()
    }
}