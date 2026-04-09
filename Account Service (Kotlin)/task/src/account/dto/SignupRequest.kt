package account.dto

data class SignupRequest(
    val name: String?,
    val lastname: String?,
    val email: String?,
    val password: String?
)