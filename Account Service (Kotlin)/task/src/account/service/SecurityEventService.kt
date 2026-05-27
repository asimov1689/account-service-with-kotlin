package account.service

import account.entity.SecurityEventEntity
import account.repository.SecurityEventRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SecurityEventService(
    private val securityEventRepository: SecurityEventRepository,
) {
    fun log(
        action: String,
        subject: String,
        obj: String,
        path: String,
    ) {
        securityEventRepository.save(
            SecurityEventEntity(
                date = LocalDate.now(),
                action = action,
                subject = subject,
                `object` = obj,
                path = path,
            ),
        )
    }
}
