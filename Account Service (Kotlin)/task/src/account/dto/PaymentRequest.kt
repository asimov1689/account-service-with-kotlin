package account.dto

data class PaymentRequest(
    val employee: String?,
    val period: String?,
    val salary: Long?
)