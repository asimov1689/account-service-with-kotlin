package account.dto

data class UserAccessRequest(
    val user: String? = null,
    val operation: String? = null,
)
