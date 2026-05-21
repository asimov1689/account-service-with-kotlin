package account.integration

import account.repository.SecurityEventRepository
import account.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.Socket
import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SslIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var securityEventRepository: SecurityEventRepository

    private val validPassword = "ValidPass1234"
    private val adminEmail = "ssladmin@acme.com"
    private val userEmail = "ssluser@acme.com"

    private fun url(path: String) = "https://localhost:$port$path"

    private fun httpsTemplate(): RestTemplate {
        // X509ExtendedTrustManager is used directly by the JDK without the hostname-verification
        // wrapper it adds around plain X509TrustManager — so this bypasses cert/hostname checks.
        val trustManager =
            object : X509ExtendedTrustManager() {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                    socket: Socket?,
                ) {}

                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                    engine: SSLEngine?,
                ) {}

                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                ) {}

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                    socket: Socket?,
                ) {}

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                    engine: SSLEngine?,
                ) {}

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                ) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        val httpClient =
            HttpClient
                .newBuilder()
                .sslContext(sslContext)
                .build()

        return RestTemplate(JdkClientHttpRequestFactory(httpClient)).apply {
            // Don't throw on 4xx/5xx — return the ResponseEntity instead
            errorHandler =
                object : ResponseErrorHandler {
                    override fun hasError(response: ClientHttpResponse) = false

                    override fun handleError(response: ClientHttpResponse) {}
                }
        }
    }

    private fun jsonHeaders() = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

    private fun authHeaders(
        username: String,
        password: String,
    ) = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setBasicAuth(username, password)
    }

    @BeforeEach
    fun setUp() {
        securityEventRepository.deleteAll()
        userRepository.deleteAll()

        val template = httpsTemplate()

        // First signup becomes ADMINISTRATOR
        template.postForEntity(
            url("/api/auth/signup"),
            HttpEntity(
                """{"name":"Admin","lastname":"Admin","email":"$adminEmail","password":"$validPassword"}""",
                jsonHeaders(),
            ),
            String::class.java,
        )

        // Second signup becomes USER
        template.postForEntity(
            url("/api/auth/signup"),
            HttpEntity(
                """{"name":"Ssl","lastname":"User","email":"$userEmail","password":"$validPassword"}""",
                jsonHeaders(),
            ),
            String::class.java,
        )
    }

    @Test
    fun `given SSL enabled when HTTPS request sent to public endpoint then TLS handshake succeeds and response is received`() {
        // Arrange
        val template = httpsTemplate()

        // Act — any HTTP response (even 4xx) proves the TLS handshake succeeded
        val response =
            template.postForEntity(
                url("/api/auth/signup"),
                HttpEntity(
                    """{"name":"New","lastname":"User","email":"newssl@acme.com","password":"$validPassword"}""",
                    jsonHeaders(),
                ),
                String::class.java,
            )

        // Assert
        assertNotNull(response)
        assertNotNull(response.statusCode)
    }

    @Test
    fun `given HTTPS with no credentials when accessing protected endpoint then returns 401`() {
        // Arrange
        val template = httpsTemplate()

        // Act
        val response = template.getForEntity(url("/api/empl/payment"), String::class.java)

        // Assert
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `given HTTPS with valid credentials when accessing protected endpoint then returns 200`() {
        // Arrange
        val template = httpsTemplate()

        // Act
        val response =
            template.exchange(
                url("/api/empl/payment"),
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(userEmail, validPassword)),
                String::class.java,
            )

        // Assert
        assertEquals(200, response.statusCode.value())
    }

    @Test
    fun `given HTTPS with wrong password when accessing protected endpoint then returns 401`() {
        // Arrange
        val template = httpsTemplate()

        // Act
        val response =
            template.exchange(
                url("/api/empl/payment"),
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(userEmail, "wrongpassword")),
                String::class.java,
            )

        // Assert
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `given plain HTTP when connecting to HTTPS port then server rejects with 400`() {
        // Arrange — plain RestTemplate with no TLS; Tomcat rejects non-TLS connections with 400
        val plainTemplate =
            RestTemplate().apply {
                errorHandler =
                    object : ResponseErrorHandler {
                        override fun hasError(response: ClientHttpResponse) = false

                        override fun handleError(response: ClientHttpResponse) {}
                    }
            }

        // Act
        val response = plainTemplate.getForEntity("http://localhost:$port/api/empl/payment", String::class.java)

        // Assert — Tomcat explicitly rejects the non-TLS request
        assertEquals(400, response.statusCode.value())
    }
}
