package account.repository

import account.entity.SecurityEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SecurityEventRepository : JpaRepository<SecurityEventEntity, Long>
