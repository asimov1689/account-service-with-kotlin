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

    private val signupUrl   = "/api/auth/signup"
    private val paymentsUrl = "/api/acct/payments"
    private val validPassword = "ValidPass1234"

    @BeforeEach
    fun setUp() {
        // Clean both tables — payments before users (FK dependency)
        paymentRepository.deleteAll()
        userRepository.deleteAll()

        // Register a known user so payment tests can reference a real employee
        val body = """{"name":"John","lastname":"Doe","email":"johndoe@acme.com","password":"$validPassword"}"""
        mockMvc.perform(
            post(signupUrl).contentType(MediaType.APPLICATION_JSON).content(body)
        )
    }

    // ─── POST PAYMENTS — SUCCESS ──────────────────────────────────────

    @Test
    fun `given valid single payment when POST payments then returns 200 with added status`() {
        // Arrange
        val body = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":123456}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
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
            {"employee":"johndoe@acme.com","period":"01-2021","salary":123456},
            {"employee":"johndoe@acme.com","period":"02-2021","salary":123456},
            {"employee":"johndoe@acme.com","period":"03-2021","salary":123456}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Added successfully!"))
    }

    @Test
    fun `given unauthenticated request when POST payments then still returns 200 (endpoint is public)`() {
        // Arrange — no httpBasic credentials
        val body = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert — endpoint is public, should not return 401
        result.andExpect(status().isOk)
    }

    // ─── POST PAYMENTS — FIELD VALIDATION FAILURES ───────────────────

    @Test
    fun `given negative salary when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":-1}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
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
        val body = """[{"employee":"johndoe@acme.com","period":"13-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `given period month 00 when POST payments then returns 400`() {
        // Arrange
        val body = """[{"employee":"johndoe@acme.com","period":"00-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given salary error at index 0 and period error at index 1 when POST payments then message contains both indexed errors`() {
        // Arrange — item 0: negative salary; item 1: invalid period; item 2: valid
        val body = """[
            {"employee":"johndoe@acme.com","period":"01-2021","salary":-1},
            {"employee":"johndoe@acme.com","period":"13-2021","salary":100000},
            {"employee":"johndoe@acme.com","period":"03-2021","salary":100000}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert — both indexed errors must appear in the message
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
        // Arrange — identical employee+period in both items
        val body = """[
            {"employee":"johndoe@acme.com","period":"01-2021","salary":100000},
            {"employee":"johndoe@acme.com","period":"01-2021","salary":200000}
        ]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    @Test
    fun `given employee-period already in DB when POST payments then returns 400`() {
        // Arrange — first POST seeds the period; second POST tries the same one
        val body = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act — second POST with same employee + period
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given employee not registered in system when POST payments then returns 400`() {
        // Arrange — ghost@acme.com was never signed up
        val body = """[{"employee":"ghost@acme.com","period":"01-2021","salary":100000}]"""

        // Act
        val result = mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    // ─── POST PAYMENTS — TRANSACTIONAL ROLLBACK ───────────────────────

    @Test
    fun `given batch with one invalid item when POST payments then rolls back and saves nothing`() {
        // Arrange — item 0 valid, item 1 has invalid salary; entire batch must fail atomically
        val body = """[
            {"employee":"johndoe@acme.com","period":"01-2021","salary":100000},
            {"employee":"johndoe@acme.com","period":"02-2021","salary":-1}
        ]"""

        // Act
        mockMvc.perform(
            post(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)

        // Assert — nothing persisted; rollback confirmed via repository
        val saved = paymentRepository.findByEmployee("johndoe@acme.com")
        assertTrue(saved.isEmpty())
    }

    // ─── PUT PAYMENTS — SUCCESS ───────────────────────────────────────

    @Test
    fun `given existing payment when PUT payments then returns 200 with updated status`() {
        // Arrange — seed a payment first
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed))

        val body = """{"employee":"johndoe@acme.com","period":"01-2021","salary":999999}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Updated successfully!"))
    }

    @Test
    fun `given salary update when PUT payments then new salary is persisted in DB`() {
        // Arrange — seed, then update
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed))

        val body = """{"employee":"johndoe@acme.com","period":"01-2021","salary":999999}"""
        mockMvc.perform(put(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(body))

        // Act — read directly from repository
        val updated = paymentRepository.findByEmployeeAndPeriod("johndoe@acme.com", "01-2021")

        // Assert
        assertEquals(999999L, updated?.salary)
    }

    @Test
    fun `given unauthenticated request when PUT payments then still returns 200 (endpoint is public)`() {
        // Arrange — seed, no credentials on PUT
        val seed = """[{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}]"""
        mockMvc.perform(post(paymentsUrl).contentType(MediaType.APPLICATION_JSON).content(seed))

        val body = """{"employee":"johndoe@acme.com","period":"01-2021","salary":200000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert — endpoint is public
        result.andExpect(status().isOk)
    }

    // ─── PUT PAYMENTS — VALIDATION FAILURES ──────────────────────────

    @Test
    fun `given negative salary when PUT payments then returns 400`() {
        // Arrange
        val body = """{"employee":"johndoe@acme.com","period":"01-2021","salary":-1}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given invalid period format when PUT payments then returns 400`() {
        // Arrange
        val body = """{"employee":"johndoe@acme.com","period":"13-2021","salary":100000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
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
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `given period not seeded in DB when PUT payments then returns 400`() {
        // Arrange — employee exists but no payment record for this period
        val body = """{"employee":"johndoe@acme.com","period":"01-2021","salary":100000}"""

        // Act
        val result = mockMvc.perform(
            put(paymentsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

        // Assert
        result.andExpect(status().isBadRequest)
    }
}