package account.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UserAccessRequest(
    @JsonProperty("user")
    val user: String? = null,

    @JsonProperty("operation")
    val operation: String? = null
)