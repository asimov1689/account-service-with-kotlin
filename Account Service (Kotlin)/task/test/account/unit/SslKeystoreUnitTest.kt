package account.unit

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.security.KeyStore

class SslKeystoreUnitTest {
    private val keystorePath = "keystore/service.p12"
    private val keystorePassword = "service"
    private val keystoreAlias = "accountant_service"

    @Test
    fun `keystore file exists on classpath`() {
        // Arrange
        val resource = ClassPathResource(keystorePath)

        // Assert
        assertTrue(resource.exists())
    }

    @Test
    fun `keystore can be loaded with configured password`() {
        // Arrange
        val keyStore = KeyStore.getInstance("PKCS12")

        // Act
        ClassPathResource(keystorePath).inputStream.use {
            keyStore.load(it, keystorePassword.toCharArray())
        }

        // Assert
        assertTrue(keyStore.size() > 0)
    }

    @Test
    fun `keystore contains entry with configured alias`() {
        // Arrange
        val keyStore = KeyStore.getInstance("PKCS12")
        ClassPathResource(keystorePath).inputStream.use {
            keyStore.load(it, keystorePassword.toCharArray())
        }

        // Assert
        assertTrue(keyStore.containsAlias(keystoreAlias))
    }
}
