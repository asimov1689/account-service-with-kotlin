package account.repository

import account.entity.SecurityEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SecurityEventRepository : JpaRepository<SecurityEventEntity, Long>