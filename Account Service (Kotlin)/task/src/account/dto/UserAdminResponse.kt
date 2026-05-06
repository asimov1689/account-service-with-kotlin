package account.dto

data class UserAdminResponse(
    val id: Long,
    val name: String,
    val lastname: String,
    val email: String,
    val roles: List<String>
)