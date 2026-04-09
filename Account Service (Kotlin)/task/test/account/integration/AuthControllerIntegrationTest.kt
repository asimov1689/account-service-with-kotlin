package account.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    // ── valid request ──────────────────────────────────────────────────────

    @Test
    fun `given valid signup request when POST signup then returns 200 with user details`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.lastname").value("Doe"))
            .andExpect(jsonPath("$.email").value("johndoe@acme.com"))
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    // ── name validation ────────────────────────────────────────────────────

    @Test
    fun `given blank name when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "",
                "lastname": "Doe",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given missing name when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "lastname": "Doe",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    // ── lastname validation ────────────────────────────────────────────────

    @Test
    fun `given blank lastname when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given missing lastname when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    // ── email validation ───────────────────────────────────────────────────

    @Test
    fun `given blank email when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given non acme email domain when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoe@google.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given email without at symbol when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoeacme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    // ── password validation ────────────────────────────────────────────────

    @Test
    fun `given blank password when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoe@acme.com",
                "password": ""
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given missing password when POST signup then returns 400 bad request`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoe@acme.com"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    // ── HTTP method validation ─────────────────────────────────────────────

    @Test
    fun `given GET request when hitting signup endpoint then returns 405 method not allowed`() {

        // Arrange - GET has no body

        // Act
        val result = mockMvc.perform(get("/api/auth/signup"))

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given PUT request when hitting signup endpoint then returns 405 method not allowed`() {

        // Arrange
        val requestBody = """
            {
                "name": "John",
                "lastname": "Doe",
                "email": "johndoe@acme.com",
                "password": "secret"
            }
        """.trimIndent()

        // Act
        val result = mockMvc.perform(
            put("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `given DELETE request when hitting signup endpoint then returns 405 method not allowed`() {

        // Arrange - DELETE has no body

        // Act
        val result = mockMvc.perform(delete("/api/auth/signup"))

        // Assert
        result.andExpect(status().isMethodNotAllowed)
    }
}