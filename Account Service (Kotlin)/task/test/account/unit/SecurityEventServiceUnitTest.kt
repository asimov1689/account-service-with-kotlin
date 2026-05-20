package account

import account.entity.SecurityEventEntity
import account.repository.SecurityEventRepository
import account.service.SecurityEventService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class SecurityEventServiceUnitTest {

    @Mock
    private lateinit var securityEventRepository: SecurityEventRepository

    @InjectMocks
    private lateinit var securityEventService: SecurityEventService

    @Test
    fun `given valid event data when log called then saves entity with correct fields`() {
        // Arrange
        val action  = "CREATE_USER"
        val subject = "Anonymous"
        val obj     = "user@acme.com"
        val path    = "/api/auth/signup"

        val captor = ArgumentCaptor.forClass(SecurityEventEntity::class.java)

        // Act
        securityEventService.log(action, subject, obj, path)

        // Assert
        verify(securityEventRepository).save(captor.capture())
        val saved = captor.value
        assertThat(saved.action).isEqualTo(action)
        assertThat(saved.subject).isEqualTo(subject)
        assertThat(saved.`object`).isEqualTo(obj)
        assertThat(saved.path).isEqualTo(path)
    }
}