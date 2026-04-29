package account.unit

import account.exception.BreachedPasswordException
import account.exception.PasswordTooShortException
import account.repository.UserRepository
import account.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.springframework.security.crypto.password.PasswordEncoder

class PasswordSecurityUnitTest {

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        // UserService needs these two deps — mock both since
        // validatePasswordSecurity() uses neither
        userService = UserService(
            mock(UserRepository::class.java),
            mock(PasswordEncoder::class.java)
        )
    }

    // ─── LENGTH CHECKS ───────────────────────────────────────────────

    @Test
    fun `given password shorter than 12 chars when validating then throws PasswordTooShortException`() {
        // Arrange
        val shortPassword = "short"

        // Act & Assert
        assertThrows<PasswordTooShortException> {
            userService.validatePasswordSecurity(shortPassword)
        }
    }

    @Test
    fun `given password with exactly 11 chars when validating then throws PasswordTooShortException`() {
        // Arrange
        val elevenChars = "Abcdefgh123" // exactly 11

        // Act & Assert
        assertThrows<PasswordTooShortException> {
            userService.validatePasswordSecurity(elevenChars)
        }
    }

    @Test
    fun `given password with exactly 12 chars when validating then no exception thrown`() {
        // Arrange
        val twelveChars = "Abcdefgh1234" // exactly 12

        // Act & Assert — must NOT throw
        userService.validatePasswordSecurity(twelveChars)
    }

    // ─── BREACHED PASSWORD CHECKS ────────────────────────────────────

    @Test
    fun `given breached password PasswordForJune when validating then throws BreachedPasswordException`() {
        // Arrange
        val breached = "PasswordForJune"

        // Act & Assert
        assertThrows<BreachedPasswordException> {
            userService.validatePasswordSecurity(breached)
        }
    }

    @Test
    fun `given all 12 breached passwords when validating then all throw BreachedPasswordException`() {
        // Arrange
        val breachedPasswords = listOf(
            "PasswordForJanuary", "PasswordForFebruary", "PasswordForMarch", "PasswordForApril",
            "PasswordForMay", "PasswordForJune", "PasswordForJuly", "PasswordForAugust",
            "PasswordForSeptember", "PasswordForOctober", "PasswordForNovember", "PasswordForDecember"
        )

        // Act & Assert
        breachedPasswords.forEach { password ->
            assertThrows<BreachedPasswordException> {
                userService.validatePasswordSecurity(password)
            }
        }
    }

    // ─── HAPPY PATH ──────────────────────────────────────────────────

    @Test
    fun `given valid password when validating then no exception thrown`() {
        // Arrange
        val validPassword = "B3Fagws6zcBa" // 12 chars, not breached

        // Act & Assert — must NOT throw
        userService.validatePasswordSecurity(validPassword)
    }
}