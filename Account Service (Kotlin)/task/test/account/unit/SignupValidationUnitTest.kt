package account.unit

import account.controller.AuthController
import account.dto.SignupRequest
import account.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class SignupValidationUnitTest {

    private lateinit var controller: AuthController

    @BeforeEach
    fun setUp() {
        // Arrange (global) — AuthController requires UserService;
        // mock it since validate() never calls it
        controller = AuthController(mock(UserService::class.java))
    }

    @Test
    fun `given blank name when validating then returns name error`() {
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
    fun `given null name when validating then returns name error`() {
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
    fun `given blank lastname when validating then returns lastname error`() {
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
    fun `given null lastname when validating then returns lastname error`() {
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
    fun `given blank email when validating then returns email blank error`() {
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
    fun `given null email when validating then returns email blank error`() {
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
    fun `given non-acme email domain when validating then returns domain error`() {
        // Arrange
        val request = SignupRequest(
            name = "John",
            lastname = "Doe",
            email = "john@gmail.com",
            password = "secret"
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertTrue(errors.contains("Email domain must be @acme.com"))
    }

    @Test
    fun `given email without at-sign when validating then returns domain error`() {
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
    fun `given blank password when validating then returns password error`() {
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
    fun `given null password when validating then returns password error`() {
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
    fun `given fully valid request when validating then returns no errors`() {
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

    @Test
    fun `given multiple invalid fields when validating then returns all errors`() {
        // Arrange
        val request = SignupRequest(
            name = "",
            lastname = "",
            email = "notvalid",
            password = ""
        )

        // Act
        val errors = controller.validate(request)

        // Assert
        assertEquals(4, errors.size)
        assertTrue(errors.contains("Name must not be blank"))
        assertTrue(errors.contains("Last name must not be blank"))
        assertTrue(errors.contains("Password must not be blank"))
    }
}