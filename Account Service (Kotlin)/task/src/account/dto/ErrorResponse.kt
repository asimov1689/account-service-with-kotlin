package account.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ErrorResponse(

    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    )
    val timestamp: LocalDateTime = LocalDateTime.now(),

    val status: Int,

    val error: String,

    val message: String,

    val path: String
)