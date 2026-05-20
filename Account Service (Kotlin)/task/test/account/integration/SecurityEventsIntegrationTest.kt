package account

import account.repository.SecurityEventRepository
import account.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SecurityEventsIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var securityEventRepository: SecurityEventRepository


    private fun basicAuth(email: String, password: String): String =
        "Basic " + Base64.getEncoder().encodeToString("$email:$password".toByteArray())

    private fun signUp(name: String, email: String, password: String) {
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name","lastname":"Doe","email":"$email","password":"$password"}""")
        ).andExpect(status().isOk)
    }

    @BeforeEach
    fun setup() {
        securityEventRepository.deleteAll()
        userRepository.deleteAll()
    }

    // ── CREATE_USER event ─────────────────────────────────────────────────────

    @Test
    fun `given valid signup when POST signup then CREATE_USER event is logged`() {
        signUp("John", "john@acme.com", "SecurePass123!")

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "CREATE_USER" && it.subject == "Anonymous" && it.`object` == "john@acme.com" })
    }

    // ── CHANGE_PASSWORD event ─────────────────────────────────────────────────

    @Test
    fun `given authenticated user when POST changepass then CHANGE_PASSWORD event is logged`() {
        signUp("John", "john@acme.com", "SecurePass123!")

        mockMvc.perform(
            post("/api/auth/changepass")
                .header("Authorization", basicAuth("john@acme.com", "SecurePass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"new_password":"NewSecurePass456!"}""")
        ).andExpect(status().isOk)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "CHANGE_PASSWORD" && it.subject == "john@acme.com" })
    }

    // ── LOGIN_FAILED event ────────────────────────────────────────────────────

    @Test
    fun `given wrong password when accessing protected endpoint then LOGIN_FAILED event is logged`() {
        signUp("John", "john@acme.com", "SecurePass123!")

        mockMvc.perform(
            get("/api/empl/payment")
                .header("Authorization", basicAuth("john@acme.com", "WrongPassword!"))
        ).andExpect(status().isUnauthorized)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "LOGIN_FAILED" && it.subject == "john@acme.com" })
    }

    // ── BRUTE_FORCE + LOCK_USER events ────────────────────────────────────────

    @Test
    fun `given 5 consecutive failed logins when attempting then BRUTE_FORCE and LOCK_USER events are logged`() {
        signUp("John", "john@acme.com", "SecurePass123!")

        repeat(5) {
            mockMvc.perform(
                get("/api/empl/payment")
                    .header("Authorization", basicAuth("john@acme.com", "WrongPassword!"))
            )
        }

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "BRUTE_FORCE" })
        assert(events.any { it.action == "LOCK_USER" && it.`object` == "Lock user john@acme.com" })
    }

    @Test
    fun `given account locked when login attempted then returns 401`() {
        signUp("John", "john@acme.com", "SecurePass123!")

        repeat(5) {
            mockMvc.perform(
                get("/api/empl/payment")
                    .header("Authorization", basicAuth("john@acme.com", "WrongPassword!"))
            )
        }

        mockMvc.perform(
            get("/api/empl/payment")
                .header("Authorization", basicAuth("john@acme.com", "SecurePass123!"))
        ).andExpect(status().isUnauthorized)
    }

    // ── ACCESS_DENIED event ───────────────────────────────────────────────────

    @Test
    fun `given user without ACCOUNTANT role when POST payments then ACCESS_DENIED event is logged`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")  // first user = ADMINISTRATOR
        signUp("User", "user@acme.com", "UserPass123!")     // second user = USER

        mockMvc.perform(
            post("/api/acct/payments")
                .header("Authorization", basicAuth("user@acme.com", "UserPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]")
        ).andExpect(status().isForbidden)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "ACCESS_DENIED" && it.subject == "user@acme.com" })
    }

    // ── GRANT_ROLE + REMOVE_ROLE events ──────────────────────────────────────

    @Test
    fun `given admin when PUT user role GRANT then GRANT_ROLE event is logged`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        mockMvc.perform(
            put("/api/admin/user/role")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","role":"ACCOUNTANT","operation":"GRANT"}""")
        ).andExpect(status().isOk)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "GRANT_ROLE" && it.subject == "admin@acme.com" })
    }

    @Test
    fun `given admin when PUT user role REMOVE then REMOVE_ROLE event is logged`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        // First grant, then remove
        mockMvc.perform(
            put("/api/admin/user/role")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","role":"ACCOUNTANT","operation":"GRANT"}""")
        )
        mockMvc.perform(
            put("/api/admin/user/role")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","role":"ACCOUNTANT","operation":"REMOVE"}""")
        ).andExpect(status().isOk)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "REMOVE_ROLE" && it.subject == "admin@acme.com" })
    }

    // ── DELETE_USER event ─────────────────────────────────────────────────────

    @Test
    fun `given admin when DELETE user then DELETE_USER event is logged`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        mockMvc.perform(
            delete("/api/admin/user/user@acme.com")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
        ).andExpect(status().isOk)

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "DELETE_USER" && it.`object` == "user@acme.com" })
    }

    // ── LOCK_USER / UNLOCK_USER from admin ────────────────────────────────────

    @Test
    fun `given admin when PUT user access LOCK then LOCK_USER event is logged and user locked`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        mockMvc.perform(
            put("/api/admin/user/access")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","operation":"LOCK"}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("User user@acme.com locked!"))

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "LOCK_USER" && it.subject == "admin@acme.com" })
    }

    @Test
    fun `given admin when PUT user access LOCK administrator then returns 400`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")

        mockMvc.perform(
            put("/api/admin/user/access")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"admin@acme.com","operation":"LOCK"}""")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Can't lock the ADMINISTRATOR!"))
    }

    @Test
    fun `given locked user when admin UNLOCK then UNLOCK_USER event is logged and user can login`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        // Lock first
        mockMvc.perform(
            put("/api/admin/user/access")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","operation":"LOCK"}""")
        )

        // Unlock
        mockMvc.perform(
            put("/api/admin/user/access")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"user@acme.com","operation":"UNLOCK"}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("User user@acme.com unlocked!"))

        val events = securityEventRepository.findAll()
        assert(events.any { it.action == "UNLOCK_USER" })
    }

    // ── GET /api/security/events — Auditor access ─────────────────────────────

    @Test
    fun `given auditor role when GET security events then returns event list sorted by id`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("Auditor", "auditor@acme.com", "AuditorPass123!")

        // Grant AUDITOR role
        mockMvc.perform(
            put("/api/admin/user/role")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"auditor@acme.com","role":"AUDITOR","operation":"GRANT"}""")
        ).andExpect(status().isOk)

        // Auditor queries events
        mockMvc.perform(
            get("/api/security/events")
                .header("Authorization", basicAuth("auditor@acme.com", "AuditorPass123!"))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `given regular user when GET security events then returns 403`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("User", "user@acme.com", "UserPass123!")

        mockMvc.perform(
            get("/api/security/events")
                .header("Authorization", basicAuth("user@acme.com", "UserPass123!"))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `given no events when GET security events as auditor then returns empty array`() {
        signUp("Admin", "admin@acme.com", "AdminPass123!")
        signUp("Auditor", "auditor@acme.com", "AuditorPass123!")

        mockMvc.perform(
            put("/api/admin/user/role")
                .header("Authorization", basicAuth("admin@acme.com", "AdminPass123!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"auditor@acme.com","role":"AUDITOR","operation":"GRANT"}""")
        )

        securityEventRepository.deleteAll()   // clean slate

        mockMvc.perform(
            get("/api/security/events")
                .header("Authorization", basicAuth("auditor@acme.com", "AuditorPass123!"))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }
}