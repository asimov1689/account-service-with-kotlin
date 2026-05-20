package account.integration

import account.dto.RoleChangeRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    private val adminEmail = "admin@acme.com"
    private val adminPass = "adminPassword99"
    private val userEmail = "user@acme.com"
    private val userPass = "userPassword99"

    @BeforeEach
    fun setup() {
        registerUser(adminEmail, adminPass)
        registerUser(userEmail, userPass)
    }

    private fun registerUser(email: String, password: String) {
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "name" to "Test",
                    "lastname" to "User",
                    "email" to email,
                    "password" to password
                )
            )
        }
    }

    @Test
    fun `given administrator when GET all users then returns 200 with sorted list`() {
        // Arrange — admin and user registered in setUp

        // Act
        val result = mockMvc.get("/api/admin/user") {
            headers { setBasicAuth(adminEmail, adminPass) }
        }

        // Assert
        result.andExpect {
            status { isOk() }
            jsonPath("$[0].email") { value(adminEmail) }
            jsonPath("$[0].roles[0]") { value("ROLE_ADMINISTRATOR") }
            jsonPath("$[1].email") { value(userEmail) }
            jsonPath("$[1].roles[0]") { value("ROLE_USER") }
        }
    }

    @Test
    fun `given user role when GET all users then returns 403 forbidden`() {
        // Arrange — non-admin user registered in setUp

        // Act
        val result = mockMvc.get("/api/admin/user") {
            headers { setBasicAuth(userEmail, userPass) }
        }

        // Assert
        result.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
            jsonPath("$.error") { value("Forbidden") }
            jsonPath("$.message") { value("Access Denied!") }
        }
    }

    @Test
    fun `given administrator when DELETE existing user then returns 200 and deleted message`() {
        // Arrange — user registered in setUp

        // Act
        val result = mockMvc.delete("/api/admin/user/$userEmail") {
            headers { setBasicAuth(adminEmail, adminPass) }
        }

        // Assert
        result.andExpect {
            status { isOk() }
            jsonPath("$.user") { value(userEmail) }
            jsonPath("$.status") { value("Deleted successfully!") }
        }
    }

    @Test
    fun `given administrator when DELETE self then returns 400`() {
        // Arrange — admin registered in setUp

        // Act
        val result = mockMvc.delete("/api/admin/user/$adminEmail") {
            headers { setBasicAuth(adminEmail, adminPass) }
        }

        // Assert
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value("Can't remove ADMINISTRATOR role!") }
        }
    }

    @Test
    fun `given administrator when DELETE nonexistent user then returns 404`() {
        // Arrange — nobody@acme.com was never registered

        // Act
        val result = mockMvc.delete("/api/admin/user/nobody@acme.com") {
            headers { setBasicAuth(adminEmail, adminPass) }
        }

        // Assert
        result.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.message") { value("User not found!") }
        }
    }

    @Test
    fun `given administrator when GRANT accountant role to user then returns 200 with updated roles`() {
        // Arrange — user registered in setUp with only ROLE_USER

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to userEmail,
                    "role" to "ACCOUNTANT",
                    "operation" to "GRANT"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isOk() }
            jsonPath("$.email") { value(userEmail) }
            jsonPath("$.roles[0]") { value("ROLE_ACCOUNTANT") }
            jsonPath("$.roles[1]") { value("ROLE_USER") }
        }
    }

    @Test
    fun `given administrator when GRANT admin role to business user then returns 400`() {
        // Arrange — user registered in setUp as a business user

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to userEmail,
                    "role" to "ADMINISTRATOR",
                    "operation" to "GRANT"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("The user cannot combine administrative and business roles!")
            }
        }
    }

    @Test
    fun `given administrator when REMOVE role user does not have then returns 400`() {
        // Arrange — user only has ROLE_USER, not ACCOUNTANT

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to userEmail,
                    "role" to "ACCOUNTANT",
                    "operation" to "REMOVE"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("The user does not have a role!") }
        }
    }

    @Test
    fun `given administrator when REMOVE only existing role then returns 400`() {
        // Arrange — user only has ROLE_USER

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to userEmail,
                    "role" to "USER",
                    "operation" to "REMOVE"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("The user must have at least one role!") }
        }
    }

    @Test
    fun `given administrator when REMOVE ADMINISTRATOR role then returns 400`() {
        // Arrange — admin registered in setUp

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to adminEmail,
                    "role" to "ADMINISTRATOR",
                    "operation" to "REMOVE"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Can't remove ADMINISTRATOR role!") }
        }
    }

    @Test
    fun `given administrator when PUT role for nonexistent user then returns 404`() {
        // Arrange — nobody@acme.com was never registered

        // Act
        val result = mockMvc.put("/api/admin/user/role") {
            headers { setBasicAuth(adminEmail, adminPass) }
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                mapOf(
                    "user" to "nobody@acme.com",
                    "role" to "USER",
                    "operation" to "GRANT"
                )
            )
        }

        // Assert
        result.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("User not found!") }
        }
    }

    @Test
    fun `given no credentials when GET admin users then returns 401`() {
        // Arrange — no credentials provided

        // Act
        val result = mockMvc.get("/api/admin/user")

        // Assert
        result.andExpect {
            status { isUnauthorized() }
        }
    }
}
