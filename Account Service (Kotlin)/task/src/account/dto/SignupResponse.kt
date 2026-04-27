package account.dto

data class SignupResponse(
    val id: Long,
    val name: String,
    val lastname: String,
    val email: String
)