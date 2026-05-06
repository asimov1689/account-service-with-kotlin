package account.dto

data class RoleChangeRequest(
    val user: String,
    val role: String,
    val operation: String
)