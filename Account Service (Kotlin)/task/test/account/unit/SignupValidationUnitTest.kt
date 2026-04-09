package account.unit

import account.controller.AuthController
import account.dto.SignupRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SignupValidationUnitTest {

    private lateinit var controller: AuthController

    @BeforeEach
    fun setUp() {
        // Arrange - shared setup for all tests
        controller = AuthController()
    }

    @Test
    fun `given blank name when validating signup request then returns name error`() {

        // Arrange
        val request = SignupRequest(
            name = "",
            lastname = "Doe",
            email = "johndoe@acme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Name must not be blank"))
    }

    @Test
    fun `given null name when validating signup request then returns name error`() {

        // Arrange
        val request = SignupRequest(
            name = null,
            lastname = "Doe",
            email = "johndoe@acme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Name must not be blank"))
    }

    @Test
    fun `given blank lastname when validating signup request then returns lastname error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "",
            email = "johndoe@acme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Last name must not be blank"))
    }

    @Test
    fun `given null lastname when validating signup request then returns lastname error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = null,
            email = "johndoe@acme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Last name must not be blank"))
    }

    @Test
    fun `given blank email when validating signup request then returns email error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Email must not be blank"))
    }

    @Test
    fun `given null email when validating signup request then returns email error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = null,
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Email must not be blank"))
    }

    @Test
    fun `given non acme email domain when validating signup request then returns domain error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "johndoe@google.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Email domain must be @acme.com"))
    }

    @Test
    fun `given email without at symbol when validating signup request then returns domain error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "johndoeacme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Email domain must be @acme.com"))
    }

    @Test
    fun `given blank password when validating signup request then returns password error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "johndoe@acme.com",
            password = ""
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Password must not be blank"))
    }

    @Test
    fun `given null password when validating signup request then returns password error`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "johndoe@acme.com",
            password = null
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Password must not be blank"))
    }

    @Test
    fun `given valid signup request when validating then returns no errors`() {

        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "johndoe@acme.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.isEmpty())
    }
}