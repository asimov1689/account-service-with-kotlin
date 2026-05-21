package account.unit

import account.entity.SecurityEventEntity
import account.repository.SecurityEventRepository
import account.service.SecurityEventService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SecurityEventServiceUnitTest {
    private lateinit var securityEventRepository: SecurityEventRepository
    private lateinit var securityEventService: SecurityEventService

    @BeforeEach
    fun setUp() {
        securityEventRepository = mock(SecurityEventRepository::class.java)
        securityEventService = SecurityEventService(securityEventRepository)
    }

    @Test
    fun `given all fields when log then saves entity with correct action`() {
        // Arrange
        val captor = ArgumentCaptor.forClass(SecurityEventEntity::class.java)

        // Act
        securityEventService.log(
            action = "CREATE_USER",
            subject = "Anonymous",
            obj = "user@acme.com",
            path = "/api/auth/signup",
        )

        // Assert
        verify(securityEventRepository).save(captor.capture())
        assertEquals("CREATE_USER", captor.value.action)
    }

    @Test
    fun `given all fields when log then saves entity with correct subject object and path`() {
        // Arrange
        val captor = ArgumentCaptor.forClass(SecurityEventEntity::class.java)

        // Act
        securityEventService.log(
            action = "LOGIN_FAILED",
            subject = "user@acme.com",
            obj = "/api/empl/payment",
            path = "/api/empl/payment",
        )

        // Assert
        verify(securityEventRepository).save(captor.capture())
        val saved = captor.value
        assertEquals("user@acme.com", saved.subject)
        assertEquals("/api/empl/payment", saved.`object`)
        assertEquals("/api/empl/payment", saved.path)
    }
}
