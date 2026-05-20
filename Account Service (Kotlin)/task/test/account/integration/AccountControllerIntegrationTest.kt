package account.integration

import account.repository.PaymentRepository
import account.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class AccountControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val signupUrl      = "/api/auth/signup"
    private val paymentsUrl    = "/api/acct/payments"
    private val adminRoleUrl   = "/api/admin/user/role"
    private val validPassword  = "ValidPass1234"
    private val adminEmail     = "admin@acme.com"
    private val adminPassword  = "AdminPass1234"
    private val acctEmail      = "johndoe@acme.com"

    @BeforeEach
    fun setUp() {
        // Clean both tables — payments before users (FK dependency)
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
                .content("""{"name":"John","lastname":"Doe","email":"$acctEmail","password":"$validPassword"}""")
        )

        // 3. Grant ACCOUNTANT role so payment endpoints are accessible
        mockMvc.perform(
            put(adminRoleUrl).contentType(MediaType.APPLICATION_JSON)
                .content("""{"user":"$acctEmail","role":"ACCOUNTANT","operation":"GRANT"}""")
                .with(httpBasic(adminEmail, adminPassword))
        )
    }

    // ─── POST PAYMENTS — SUCCESS ──────────────────────────────────────

    @Test
    fun `given valid single payment when POST payments then returns 200 with added status`() {
        // Arrange
        val body = """[{"employee":"$acctEmail","period":"01-2021","salary":123456}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Added successfully!"))
    }

    @Test
    fun `given valid batch of three different periods when POST payments then returns 200 with added status`() {
        // Arrange
        val body = """[
            {"employee":"$acctEmail","period":"01-2021","salary":123456},
            {"employee":"$acctEmail","period":"02-2021","salary":123456},
            {"employee":"$acctEmail","period":"03-2021","salary":123456}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Added successfully!"))
    }

    @Test
    fun `given unauthenticated request when POST payments then returns 401`() {
        // Arrange
        val body = """[{"employee":"$acctEmail","period":"01-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    // ─── POST PAYMENTS — FIELD VALIDATION FAILURES ───────────────────

    @Test
    fun `given negative salary when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"$acctEmail","period":"01-2021","salary":-1}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value(paymentsUrl))
    }

    @Test
    fun `given period month 13 when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"$acctEmail","period":"13-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `given period month 00 when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"$acctEmail","period":"00-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given salary error at index 0 and period error at index 1 when POST payments then message contains both indexed errors`() {
        // Arrange
        val body = """[
            {"employee":"$acctEmail","period":"01-2021","salary":-1},
            {"employee":"$acctEmail","period":"13-2021","salary":100000},
            {"employee":"$acctEmail","period":"03-2021","salary":100000}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("payments[0].salary")
            ))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("payments[1].period")
            ))
    }

    // ─── POST PAYMENTS — BUSINESS RULE FAILURES ──────────────────────

    @Test
    fun `given duplicate employee-period pair within same batch when POST payments then returns 400`() {
        // Arrange
        val body = """[
            {"employee":"$acctEmail","period":"01-2021","salary":100000},
            {"employee":"$acctEmail","period":"01-2021","salary":200000}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given employee-period already in DB when POST payments then returns 400`() {
        // Arrange — seed the record first
        val body = """[{"employee":"$acctEmail","period":"01-2021","salary":100000}]"""
        mockMvc.perform(
            post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Act — attempt to add the same period again
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given employee not registered in system when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"ghost@acme.com","period":"01-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    // ─── POST PAYMENTS — TRANSACTIONAL ROLLBACK ───────────────────────

    @Test
    fun `given batch with one invalid item when POST payments then rolls back and saves nothing`() {
        // Arrange
        val body = """[
            {"employee":"$acctEmail","period":"01-2021","salary":100000},
            {"employee":"$acctEmail","period":"02-2021","salary":-1}
        ]"""

        // Act
        mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        ).andExpect(status().isBadRequest)

        // Assert — nothing persisted
        val saved = paymentRepository.findByEmployee(acctEmail)
        assertTrue(saved.isEmpty())
    }

    // ─── PUT PAYMENTS — SUCCESS ───────────────────────────────────────

    @Test
    fun `given existing payment when PUT payments then returns 200 with updated status`() {
        // Arrange — seed the record
        val seed = """[{"employee":"$acctEmail","period":"01-2021","salary":100000}]"""
        mockMvc.perform(
            post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(acctEmail, validPassword))
        )
        val body = """{"employee":"$acctEmail","period":"01-2021","salary":999999}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Updated successfully!"))
    }

    @Test
    fun `given salary update when PUT payments then new salary is persisted in DB`() {
        // Arrange — seed, then update
        val seed = """[{"employee":"$acctEmail","period":"01-2021","salary":100000}]"""
        mockMvc.perform(
            post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(acctEmail, validPassword))
        )
        val body = """{"employee":"$acctEmail","period":"01-2021","salary":999999}"""
        mockMvc.perform(
            put(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Act
        val updated = paymentRepository.findByEmployeeAndPeriod(acctEmail, "01-2021")

        // Assert
        assertEquals(999999L, updated?.salary)
    }

    @Test
    fun `given unauthenticated request when PUT payments then returns 401`() {
        // Arrange — seed a record so the endpoint would otherwise succeed
        val seed = """[{"employee":"$acctEmail","period":"01-2021","salary":100000}]"""
        mockMvc.perform(
            post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed)
                .with(httpBasic(acctEmail, validPassword))
        )
        val body = """{"employee":"$acctEmail","period":"01-2021","salary":200000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isUnauthorized)
    }

    // ─── PUT PAYMENTS — VALIDATION FAILURES ──────────────────────────

    @Test
    fun `given negative salary when PUT payments then returns 400`() {
        // Arrange
        val body = """{"employee":"$acctEmail","period":"01-2021","salary":-1}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given invalid period format when PUT payments then returns 400`() {
        // Arrange
        val body = """{"employee":"$acctEmail","period":"13-2021","salary":100000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given employee not registered when PUT payments then returns 400`() {
        // Arrange
        val body = """{"employee":"ghost@acme.com","period":"01-2021","salary":100000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given period not seeded in DB when PUT payments then returns 400`() {
        // Arrange — no payment record exists for this period
        val body = """{"employee":"$acctEmail","period":"01-2021","salary":100000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpBasic(acctEmail, validPassword))
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }
}
