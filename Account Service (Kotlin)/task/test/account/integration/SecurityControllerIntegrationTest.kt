package account.integration

import account.repository.SecurityEventRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var securityEventRepository: SecurityEventRepository

    private val signupUrl = "/api/auth/signup"
    private val eventsUrl = "/api/security/events"
    private val adminRoleUrl = "/api/admin/user/role"
    private val validPassword = "ValidPass1234"
    private val adminEmail = "admin@acme.com"
    private val auditorEmail = "auditor@acme.com"
    private val userEmail = "user@acme.com"

    @BeforeEach
    fun setUp() {
        securityEventRepository.deleteAll()
        userRepository.deleteAll()

        // First signup becomes ADMINISTRATOR
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Admin","lastname":"Admin","email":"$adminEmail","password":"$validPassword"}"""),
        )

        // Second signup becomes USER
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Auditor","lastname":"Test","email":"$auditorEmail","password":"$validPassword"}"""),
        )

        // Grant AUDITOR role
        mockMvc.perform(
            put(adminRoleUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"$auditorEmail","role":"AUDITOR","operation":"GRANT"}""")
                .with(httpBasic(adminEmail, validPassword)),
        )
    }

    @Test
    fun `given no credentials when GET events then returns 401`() {
        // Arrange — no httpBasic

        // Act
        val result = mockMvc.perform(get(eventsUrl))

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given USER role when GET events then returns 403`() {
        // Arrange
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Regular","lastname":"User","email":"$userEmail","password":"$validPassword"}"""),
        )

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(userEmail, validPassword)),
            )

        // Assert
        result.andExpect(status().isForbidden)
    }

    @Test
    fun `given ADMINISTRATOR role when GET events then returns 403`() {
        // Arrange — ADMINISTRATOR does not have AUDITOR role

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(adminEmail, validPassword)),
            )

        // Assert
        result.andExpect(status().isForbidden)
    }

    @Test
    fun `given AUDITOR role when GET events then returns 200`() {
        // Arrange — AUDITOR role granted in setUp

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert
        result.andExpect(status().isOk)
    }

    @Test
    fun `given AUDITOR role when GET events then response is an array`() {
        // Arrange

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `given events logged when GET events then each event has all required fields`() {
        // Arrange — setUp already produced events

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].date").exists())
            .andExpect(jsonPath("$[0].action").exists())
            .andExpect(jsonPath("$[0].subject").exists())
            .andExpect(jsonPath("$[0].object").exists())
            .andExpect(jsonPath("$[0].path").exists())
    }

    @Test
    fun `given signup when GET events then CREATE_USER event is logged`() {
        // Arrange — clear events then trigger one fresh signup
        securityEventRepository.deleteAll()
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New","lastname":"User","email":"$userEmail","password":"$validPassword"}"""),
        )

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].action").value("CREATE_USER"))
            .andExpect(jsonPath("$[0].subject").value("Anonymous"))
            .andExpect(jsonPath("$[0].object").value(userEmail))
            .andExpect(jsonPath("$[0].path").value(signupUrl))
    }

    @Test
    fun `given wrong password when accessing endpoint then LOGIN_FAILED event is logged`() {
        // Arrange — clear events then trigger a failed auth attempt
        securityEventRepository.deleteAll()
        mockMvc.perform(
            get(eventsUrl).with(httpBasic(auditorEmail, "wrongpassword")),
        )

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].action").value("LOGIN_FAILED"))
            .andExpect(jsonPath("$[0].subject").value(auditorEmail))
    }

    @Test
    fun `given multiple events when GET events then returned in ascending id order`() {
        // Arrange — setUp logged: CREATE_USER(admin), CREATE_USER(auditor), GRANT_ROLE(auditor)

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert — first two events are both CREATE_USER (signup order preserved)
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].action").value("CREATE_USER"))
            .andExpect(jsonPath("$[1].action").value("CREATE_USER"))
            .andExpect(jsonPath("$[2].action").value("GRANT_ROLE"))
    }

    @Test
    fun `given five consecutive failed logins when GET events then BRUTE_FORCE and LOCK_USER events are logged`() {
        // Arrange — clear events; register victim; fail login 5 times
        securityEventRepository.deleteAll()
        mockMvc.perform(
            post(signupUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Victim","lastname":"User","email":"$userEmail","password":"$validPassword"}"""),
        )
        securityEventRepository.deleteAll()

        repeat(5) {
            mockMvc.perform(
                get("/api/empl/payment").with(httpBasic(userEmail, "wrongpassword")),
            )
        }

        // Act
        val result =
            mockMvc.perform(
                get(eventsUrl).with(httpBasic(auditorEmail, validPassword)),
            )

        // Assert — after 5 failures: 5× LOGIN_FAILED + BRUTE_FORCE + LOCK_USER
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[5].action").value("BRUTE_FORCE"))
            .andExpect(jsonPath("$[6].action").value("LOCK_USER"))
    }
}
