package account.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChangePassRequest(
    @field:JsonProperty("new_password")
    val newPassword: String?,
)
