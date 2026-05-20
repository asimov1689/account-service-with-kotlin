package account.controller

import account.entity.SecurityEventEntity
import account.repository.SecurityEventRepository
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/security")
class SecurityController(
    private val securityEventRepository: SecurityEventRepository
) {

    @GetMapping("/events")
    fun getEvents(): ResponseEntity<List<SecurityEventEntity>> {
        val events = securityEventRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
        return ResponseEntity.ok(events)
    }
}