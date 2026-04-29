package account.integration

import account.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("SpellCheckingInspection")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanUp() {
        userRepository.deleteAll()
    }

    private val signupUrl = "/api/auth/signup"
    private val changePassUrl = "/api/auth/changepass"
    private val paymentUrl = "/api/empl/payment/"

    // valid password reused across tests — 12 chars, not breached
    private val validPassword = "ValidPass1234"

    // ─── SIGNUP — SUCCESS ────────────────────────────────────────────

    @Test
    fun `given valid signup request when POST signup then returns 200 with id and user details`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.lastname").value("Doe"))
            .andExpect(jsonPath("$.email").value("johndoe@acme.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    @Test
    fun `given mixed case email when POST signup then response email is stored lowercase`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"JohnDoe@acme.com","password":"$validPassword"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("johndoe@acme.com"))
    }

    // ─── SIGNUP — DUPLICATE ──────────────────────────────────────────

    @Test
    fun `given duplicate email when POST signup then returns 400 with user exist message`() {
        // Arrange — register first user
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act — attempt duplicate
        val result = mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("User exist!"))
    }

    @Test
    fun `given duplicate email with different case when POST signup then returns 400`() {
        // Arrange
        val original = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(original))

        // Act
        val duplicate = """{"name":"John","lastname":"Doe","email":"JohnDoe@acme.com","password":"$validPassword"}"""
        val result = mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(duplicate)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("User exist!"))
    }

    // ─── SIGNUP — PASSWORD SECURITY (NEW IN STAGE 003) ───────────────

    @Test
    fun `given password shorter than 12 chars when POST signup then returns 400 with length message`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"short"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Password length must be 12 chars minimum!"))
    }

    @Test
    fun `given breached password when POST signup then returns 400 with breached message`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"PasswordForJune"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("The password is in the hacker's database!"))
    }

    // ─── SIGNUP — VALIDATION FAILURES ───────────────────────────────

    @Test
    fun `given blank name when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""

        // Act & Assert
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank lastname when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"","email":"johndoe@acme.com","password":"$validPassword"}"""

        // Act & Assert
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank email when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"","password":"$validPassword"}"""

        // Act & Assert
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `given non-acme email domain when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"john@gmail.com","password":"$validPassword"}"""

        // Act & Assert
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank password when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":""}"""

        // Act & Assert
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    // ─── SIGNUP — WRONG HTTP METHODS ────────────────────────────────

    @Test
    fun `given GET on signup endpoint then returns 405`() {
        mockMvc.perform(get(signupUrl)).andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given PUT on signup endpoint then returns 405`() {
        mockMvc.perform(put(signupUrl)).andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given DELETE on signup endpoint then returns 405`() {
        mockMvc.perform(delete(signupUrl)).andExpect(status().isMethodNotAllowed)
    }

    // ─── PAYMENT — SUCCESS ───────────────────────────────────────────

    @Test
    fun `given valid credentials when GET payment then returns 200 with user details`() {
        // Arrange — register user first
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            get(paymentUrl).with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.lastname").value("Doe"))
            .andExpect(jsonPath("$.email").value("johndoe@acme.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    @Test
    fun `given mixed case email credentials when GET payment then returns 200`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act & Assert
        mockMvc.perform(get(paymentUrl).with(httpBasic("JohnDoe@acme.com", validPassword)))
            .andExpect(status().isOk)
    }

    // ─── PAYMENT — AUTH FAILURES ─────────────────────────────────────

    @Test
    fun `given no credentials when GET payment then returns 401`() {
        mockMvc.perform(get(paymentUrl)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `given wrong password when GET payment then returns 401`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act & Assert
        mockMvc.perform(get(paymentUrl).with(httpBasic("johndoe@acme.com", "wrongpassword")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `given unknown user when GET payment then returns 401`() {
        mockMvc.perform(get(paymentUrl).with(httpBasic("nobody@acme.com", validPassword)))
            .andExpect(status().isUnauthorized)
    }

    // ─── CHANGEPASS — SUCCESS (NEW IN STAGE 003) ─────────────────────

    @Test
    fun `given valid new password when POST changepass then returns 200 with email and status`() {
        // Arrange — register user
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"bZPGqH7fTJWW"}""")
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("johndoe@acme.com"))
            .andExpect(jsonPath("$.status").value("The password has been updated successfully"))
    }

    @Test
    fun `given changed password when authenticating again then new password works`() {
        // Arrange — register then change password
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        val newPassword = "bZPGqH7fTJWW"
        mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"$newPassword"}""")
        )

        // Act — authenticate with new password
        val result = mockMvc.perform(
            get(paymentUrl).with(httpBasic("johndoe@acme.com", newPassword))
        )

        // Assert — new password works, old one would return 401
        result.andExpect(status().isOk)
    }

    // ─── CHANGEPASS — VALIDATION FAILURES (NEW IN STAGE 003) ─────────

    @Test
    fun `given same password when POST changepass then returns 400 with identical message`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"$validPassword"}""")
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("The passwords must be different!"))
    }

    @Test
    fun `given short new password when POST changepass then returns 400 with length message`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"short"}""")
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Password length must be 12 chars minimum!"))
    }

    @Test
    fun `given breached new password when POST changepass then returns 400 with breached message`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"PasswordForJuly"}""")
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("The password is in the hacker's database!"))
    }

    @Test
    fun `given blank new password when POST changepass then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":""}""")
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    // ─── CHANGEPASS — AUTH FAILURES (NEW IN STAGE 003) ───────────────

    @Test
    fun `given unauthenticated request when POST changepass then returns 401`() {
        // Arrange — no credentials
        val result = mockMvc.perform(
            post(changePassUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"bZPGqH7fTJWW"}""")
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given wrong credentials when POST changepass then returns 401`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act
        val result = mockMvc.perform(
            post(changePassUrl)
                .with(httpBasic("johndoe@acme.com", "wrongpassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"bZPGqH7fTJWW"}""")
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }
}