package account.integration

import account.repository.PaymentRepository
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
class PaymentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val signupUrl    = "/api/auth/signup"
    private val paymentsUrl  = "/api/acct/payments"
    private val paymentUrl   = "/api/empl/payment"
    private val adminRoleUrl = "/api/admin/user/role"
    private val validPassword = "ValidPass1234"
    private val adminEmail    = "admin@acme.com"
    private val adminPassword = "AdminPass1234"
    private val userEmail     = "johndoe@acme.com"

    @BeforeEach
    fun setUp() {
        paymentRepository.deleteAll()
        userRepository.deleteAll()

        // 1. Register admin (first user → ROLE_ADMINISTRATOR)
        mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Admin","lastname":"User","email":"$adminEmail","password":"$adminPassword"}""")
        )

        // 2. Register johndoe (second user → ROLE_USER)
        mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"John","lastname":"Doe","email":"$userEmail","password":"$validPassword"}""")
        )

        // 3. Grant ACCOUNTANT role so seeding via POST /api/acct/payments works
        mockMvc.perform(
            put(adminRoleUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"$userEmail","role":"ACCOUNTANT","operation":"GRANT"}""")
                .with(httpBasic(adminEmail, adminPassword))
        )
    }

    // ─── AUTH FAILURES (moved here from AuthControllerIntegrationTest) ─

    @Test
    fun `given no credentials when GET payment then returns 401`() {
        // Arrange — no httpBasic

        // Act
        val result = mockMvc.perform(get(paymentUrl))

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given wrong password when GET payment then returns 401`() {
        // Arrange

        // Act
        val result = mockMvc.perform(
            get(paymentUrl).with(httpBasic("johndoe@acme.com", "wrongpassword"))
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    @Test
    fun `given unknown user when GET payment then returns 401`() {
        // Arrange

        // Act
        val result = mockMvc.perform(
            get(paymentUrl).with(httpBasic("nobody@acme.com", validPassword))
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    // ─── GET WITH PERIOD — SUCCESS ────────────────────────────────────

    @Test
    fun `given valid period with seeded payment when GET payment then returns 200 with formatted salary`() {
        // Arrange
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":123456}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=01-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.lastname").value("Doe"))
            .andExpect(jsonPath("$.period").value("January-2021"))
            .andExpect(jsonPath("$.salary").value("1234 dollar(s) 56 cent(s)"))
    }

    @Test
    fun `given salary with no cents when GET payment then formats as 0 cents`() {
        // Arrange — 100000 cents = 1000 dollars exactly
        val seed = """[{"employee":"johndoe@acme.com","period":"06-2022","salary":100000}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=06-2022")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.period").value("June-2022"))
            .andExpect(jsonPath("$.salary").value("1000 dollar(s) 0 cent(s)"))
    }

    @Test
    fun `given valid period with no payment record when GET payment then returns 200 with empty body`() {
        // Arrange — no payment seeded for this period

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=01-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert — 200 with empty object, not 404
        result.andExpect(status().isOk)
    }

    @Test
    fun `given valid period when GET payment then response does not contain password field`() {
        // Arrange
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":123456}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=01-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert — password must never appear in any salary response
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    // ─── GET WITH PERIOD — VALIDATION FAILURES ───────────────────────

    @Test
    fun `given period month 13 when GET payment then returns 400`() {
        // Arrange

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=13-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given period month 00 when GET payment then returns 400`() {
        // Arrange

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=00-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given single digit month without leading zero when GET payment then returns 400`() {
        // Arrange

        // Act
        val result = mockMvc.perform(
            get("$paymentUrl?period=1-2021")
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    // ─── GET WITHOUT PERIOD — SUCCESS ─────────────────────────────────

    @Test
    fun `given no period param with no records when GET payment then returns 200 empty array`() {
        // Arrange — no payments seeded

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `given no period param with three records when GET payment then returns sorted descending by date`() {
        // Arrange — seed three periods out of order
        val seed = """[
            {"employee":"johndoe@acme.com","period":"01-2021","salary":100000},
            {"employee":"johndoe@acme.com","period":"03-2021","salary":300000},
            {"employee":"johndoe@acme.com","period":"02-2021","salary":200000}
        ]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert — March, February, January (descending)
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].period").value("March-2021"))
            .andExpect(jsonPath("$[1].period").value("February-2021"))
            .andExpect(jsonPath("$[2].period").value("January-2021"))
    }

    @Test
    fun `given no period param with records across two years when GET payment then later year comes first`() {
        // Arrange
        val seed = """[
            {"employee":"johndoe@acme.com","period":"01-2021","salary":100000},
            {"employee":"johndoe@acme.com","period":"01-2022","salary":200000},
            {"employee":"johndoe@acme.com","period":"01-2020","salary":150000}
        ]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert — 2022, 2021, 2020
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].period").value("January-2022"))
            .andExpect(jsonPath("$[1].period").value("January-2021"))
            .andExpect(jsonPath("$[2].period").value("January-2020"))
    }

    @Test
    fun `given no period param when GET payment then each item contains all four required fields`() {
        // Arrange
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":123456}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(userEmail, validPassword)))

        // Act
        val result = mockMvc.perform(
            get(paymentUrl)
                .with(httpBasic("johndoe@acme.com", validPassword))
        )

        // Assert — name, lastname, period, salary all present; password absent
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("John"))
            .andExpect(jsonPath("$[0].lastname").value("Doe"))
            .andExpect(jsonPath("$[0].period").value("January-2021"))
            .andExpect(jsonPath("$[0].salary").value("1234 dollar(s) 56 cent(s)"))
            .andExpect(jsonPath("$[0].password").doesNotExist())
    }
}