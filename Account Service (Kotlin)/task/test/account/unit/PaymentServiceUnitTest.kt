package account.unit

import account.dto.PaymentRequest
import account.entity.PaymentEntity
import account.entity.UserEntity
import account.exception.PaymentValidationException
import account.repository.PaymentRepository
import account.repository.UserRepository
import account.service.PaymentService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PaymentServiceUnitTest {

    private lateinit var paymentService: PaymentService
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        // Arrange (global) — PaymentService requires both repos; mock them
        paymentRepository = mock(PaymentRepository::class.java)
        userRepository    = mock(UserRepository::class.java)
        paymentService    = PaymentService(paymentRepository, userRepository)
    }

    // ─── isValidPeriod ────────────────────────────────────────────────

    @Test
    fun `given period 01-2021 when checking isValidPeriod then returns true`() {
        // Arrange
        val period = "01-2021"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `given period 12-2099 when checking isValidPeriod then returns true`() {
        // Arrange
        val period = "12-2099"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `given month 00 when checking isValidPeriod then returns false`() {
        // Arrange
        val period = "00-2021"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `given month 13 when checking isValidPeriod then returns false`() {
        // Arrange
        val period = "13-2021"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `given single digit month without leading zero when checking isValidPeriod then returns false`() {
        // Arrange
        val period = "1-2021"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `given three digit year when checking isValidPeriod then returns false`() {
        // Arrange
        val period = "01-202"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `given slash separator instead of hyphen when checking isValidPeriod then returns false`() {
        // Arrange
        val period = "01/2021"

        // Act
        val result = paymentService.isValidPeriod(period)

        // Assert
        assertFalse(result)
    }

    // ─── formatSalary ─────────────────────────────────────────────────

    @Test
    fun `given salary 123456 cents when formatting then returns 1234 dollars 56 cents`() {
        // Arrange
        val salary = 123456L

        // Act
        val result = paymentService.formatSalary(salary)

        // Assert
        assertEquals("1234 dollar(s) 56 cent(s)", result)
    }

    @Test
    fun `given salary 100 cents when formatting then returns 1 dollar 0 cents`() {
        // Arrange
        val salary = 100L

        // Act
        val result = paymentService.formatSalary(salary)

        // Assert
        assertEquals("1 dollar(s) 0 cent(s)", result)
    }

    @Test
    fun `given salary 0 cents when formatting then returns 0 dollars 0 cents`() {
        // Arrange
        val salary = 0L

        // Act
        val result = paymentService.formatSalary(salary)

        // Assert
        assertEquals("0 dollar(s) 0 cent(s)", result)
    }

    @Test
    fun `given salary 99 cents when formatting then returns 0 dollars 99 cents`() {
        // Arrange
        val salary = 99L

        // Act
        val result = paymentService.formatSalary(salary)

        // Assert
        assertEquals("0 dollar(s) 99 cent(s)", result)
    }

    // ─── formatPeriod ─────────────────────────────────────────────────

    @Test
    fun `given period 01-2021 when formatting then returns January-2021`() {
        // Arrange
        val period = "01-2021"

        // Act
        val result = paymentService.formatPeriod(period)

        // Assert
        assertEquals("January-2021", result)
    }

    @Test
    fun `given period 06-2022 when formatting then returns June-2022`() {
        // Arrange
        val period = "06-2022"

        // Act
        val result = paymentService.formatPeriod(period)

        // Assert
        assertEquals("June-2022", result)
    }

    @Test
    fun `given period 12-2023 when formatting then returns December-2023`() {
        // Arrange
        val period = "12-2023"

        // Act
        val result = paymentService.formatPeriod(period)

        // Assert
        assertEquals("December-2023", result)
    }

    // ─── addPayments — Phase 1: field-level validation ────────────────

    @Test
    fun `given negative salary at index 0 when addPayments then throws with payments-0 indexed message`() {
        // Arrange
        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = -1L)
        )

        // Act
        val ex = assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }

        // Assert
        assertTrue(ex.message!!.contains("payments[0].salary"))
    }

    @Test
    fun `given invalid period month 13 at index 1 when addPayments then throws with payments-1 indexed message`() {
        // Arrange
        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = 100000L),
            PaymentRequest(employee = "johndoe@acme.com", period = "13-2021", salary = 100000L)
        )

        // Act
        val ex = assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }

        // Assert
        assertTrue(ex.message!!.contains("payments[1].period"))
    }

    @Test
    fun `given salary error at index 0 and period error at index 1 when addPayments then message contains both indexed errors`() {
        // Arrange
        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = -1L),
            PaymentRequest(employee = "johndoe@acme.com", period = "13-2021", salary = 100000L)
        )

        // Act
        val ex = assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }

        // Assert
        assertTrue(ex.message!!.contains("payments[0].salary"))
        assertTrue(ex.message!!.contains("payments[1].period"))
    }

    // ─── addPayments — Phase 2: within-batch duplicate check ──────────

    @Test
    fun `given same employee-period pair twice in batch when addPayments then throws PaymentValidationException`() {
        // Arrange — two items with identical employee + period
        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = 100000L),
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = 200000L)
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }
    }

    @Test
    fun `given same employee different periods in batch when addPayments then does not throw on phase 2`() {
        // Arrange — same employee, different periods (not a duplicate)
        // Mock repos to make Phase 3 pass so we can confirm Phase 2 does not reject this
        val user = UserEntity(id = 1L, name = "John", lastname = "Doe",
            email = "johndoe@acme.com", password = "hashed")
        `when`(userRepository.findByEmail("johndoe@acme.com")).thenReturn(user)
        `when`(paymentRepository.findByEmployeeAndPeriod("johndoe@acme.com", "01-2021"))
            .thenReturn(null)
        `when`(paymentRepository.findByEmployeeAndPeriod("johndoe@acme.com", "02-2021"))
            .thenReturn(null)

        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = 100000L),
            PaymentRequest(employee = "johndoe@acme.com", period = "02-2021", salary = 200000L)
        )

        // Act & Assert — must not throw
        paymentService.addPayments(payments)
    }

    // ─── addPayments — Phase 3: business rule checks ──────────────────

    @Test
    fun `given employee not in user table when addPayments then throws PaymentValidationException`() {
        // Arrange — mock returns null (employee not found)
        `when`(userRepository.findByEmail("ghost@acme.com")).thenReturn(null)
        val payments = listOf(
            PaymentRequest(employee = "ghost@acme.com", period = "01-2021", salary = 100000L)
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }
    }

    @Test
    fun `given employee-period already in DB when addPayments then throws PaymentValidationException`() {
        // Arrange — employee found, but period already has a record
        val user = UserEntity(id = 1L, name = "John", lastname = "Doe",
            email = "johndoe@acme.com", password = "hashed")
        val existing = PaymentEntity(id = 1L, employee = "johndoe@acme.com",
            period = "01-2021", salary = 100000L)
        `when`(userRepository.findByEmail("johndoe@acme.com")).thenReturn(user)
        `when`(paymentRepository.findByEmployeeAndPeriod("johndoe@acme.com", "01-2021"))
            .thenReturn(existing)

        val payments = listOf(
            PaymentRequest(employee = "johndoe@acme.com", period = "01-2021", salary = 100000L)
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.addPayments(payments)
        }
    }

    // ─── updatePayment — validation failures ──────────────────────────

    @Test
    fun `given negative salary when updatePayment then throws PaymentValidationException`() {
        // Arrange
        val request = PaymentRequest(
            employee = "johndoe@acme.com", period = "01-2021", salary = -1L
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.updatePayment(request)
        }
    }

    @Test
    fun `given invalid period format when updatePayment then throws PaymentValidationException`() {
        // Arrange
        val request = PaymentRequest(
            employee = "johndoe@acme.com", period = "13-2021", salary = 100000L
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.updatePayment(request)
        }
    }

    @Test
    fun `given unknown employee when updatePayment then throws PaymentValidationException`() {
        // Arrange
        `when`(userRepository.findByEmail("nobody@acme.com")).thenReturn(null)
        val request = PaymentRequest(
            employee = "nobody@acme.com", period = "01-2021", salary = 100000L
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.updatePayment(request)
        }
    }

    @Test
    fun `given period not in DB when updatePayment then throws PaymentValidationException`() {
        // Arrange — employee exists but no payment record for that period
        val user = UserEntity(id = 1L, name = "John", lastname = "Doe",
            email = "johndoe@acme.com", password = "hashed")
        `when`(userRepository.findByEmail("johndoe@acme.com")).thenReturn(user)
        `when`(paymentRepository.findByEmployeeAndPeriod("johndoe@acme.com", "01-2021"))
            .thenReturn(null)

        val request = PaymentRequest(
            employee = "johndoe@acme.com", period = "01-2021", salary = 100000L
        )

        // Act & Assert
        assertThrows<PaymentValidationException> {
            paymentService.updatePayment(request)
        }
    }

    // ─── getAllPayments — descending sort ─────────────────────────────

    @Test
    fun `given payments in unsorted order when getAllPayments then returns sorted by date descending`() {
        // Arrange — repo returns payments in wrong order
        val jan = PaymentEntity(id = 1L, employee = "johndoe@acme.com",
            period = "01-2021", salary = 100000L)
        val mar = PaymentEntity(id = 2L, employee = "johndoe@acme.com",
            period = "03-2021", salary = 300000L)
        val feb = PaymentEntity(id = 3L, employee = "johndoe@acme.com",
            period = "02-2021", salary = 200000L)
        `when`(paymentRepository.findByEmployee("johndoe@acme.com"))
            .thenReturn(listOf(jan, mar, feb))

        // Act
        val result = paymentService.getAllPayments("johndoe@acme.com")

        // Assert — descending: March, February, January
        assertEquals("03-2021", result[0].period)
        assertEquals("02-2021", result[1].period)
        assertEquals("01-2021", result[2].period)
    }

    @Test
    fun `given payments across two years when getAllPayments then later year comes first`() {
        // Arrange
        val jan2021 = PaymentEntity(id = 1L, employee = "johndoe@acme.com",
            period = "01-2021", salary = 100000L)
        val jan2022 = PaymentEntity(id = 2L, employee = "johndoe@acme.com",
            period = "01-2022", salary = 200000L)
        `when`(paymentRepository.findByEmployee("johndoe@acme.com"))
            .thenReturn(listOf(jan2021, jan2022))

        // Act
        val result = paymentService.getAllPayments("johndoe@acme.com")

        // Assert — 2022 before 2021
        assertEquals("01-2022", result[0].period)
        assertEquals("01-2021", result[1].period)
    }
}