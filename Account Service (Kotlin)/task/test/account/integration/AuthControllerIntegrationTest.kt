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
    private val paymentUrl = "/api/empl/payment/"

    // ─── SIGNUP — SUCCESS ────────────────────────────────────────────

    @Test
    fun `given valid signup request when POST signup then returns 200 with id and user details`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""

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
        val body = """{"name":"John","lastname":"Doe","email":"JohnDoe@acme.com","password":"secret123"}"""

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
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Act — attempt duplicate registration
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("User exist!"))
    }

    @Test
    fun `given duplicate email with different case when POST signup then returns 400`() {
        // Arrange — register with lowercase email
        val originalBody = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(originalBody)
        )

        // Act — attempt duplicate with mixed case email
        val duplicateBody = """{"name":"John","lastname":"Doe","email":"JohnDoe@acme.com","password":"secret123"}"""
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("User exist!"))
    }

    // ─── SIGNUP — VALIDATION FAILURES ───────────────────────────────

    @Test
    fun `given blank name when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"","lastname":"Doe","email":"johndoe@acme.com","password":"secret"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank lastname when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"","email":"johndoe@acme.com","password":"secret"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank email when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"","password":"secret"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given non-acme email domain when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"john@gmail.com","password":"secret"}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given blank password when POST signup then returns 400`() {
        // Arrange
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":""}"""

        // Act
        val result = mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    // ─── SIGNUP — WRONG HTTP METHODS ────────────────────────────────

    @Test
    fun `given GET on signup endpoint then returns 405`() {
        // Arrange — no body needed, wrong method is the condition

        // Act
        val result = mockMvc.perform(get(signupUrl))

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given PUT on signup endpoint then returns 405`() {
        // Arrange — no body needed, wrong method is the condition

        // Act
        val result = mockMvc.perform(put(signupUrl))

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given DELETE on signup endpoint then returns 405`() {
        // Arrange — no body needed, wrong method is the condition

        // Act
        val result = mockMvc.perform(delete(signupUrl))

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }

    // ─── PAYMENT — SUCCESS ───────────────────────────────────────────

    @Test
    fun `given valid credentials when GET payment then returns 200 with user details`() {
        // Arrange — register user first
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", "secret123"))
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
        // Arrange — register with lowercase email
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Act — authenticate with mixed case email
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("JohnDoe@acme.com", "secret123"))
        )

        // Assert
        result.andExpect(status().isOk)
    }

    // ─── PAYMENT — AUTH FAILURES ─────────────────────────────────────

    @Test
    fun `given no credentials when GET payment then returns 401`() {
        // Arrange — no setup needed, absence of credentials is the condition

        // Act
        val result = mockMvc.perform(get(paymentUrl))

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given wrong password when GET payment then returns 401`() {
        // Arrange — register user
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"secret123"}"""
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Act — authenticate with wrong password
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", "wrongpassword"))
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given unknown user credentials when GET payment then returns 401`() {
        // Arrange — no user registered

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("nobody@acme.com", "secret123"))
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }
}