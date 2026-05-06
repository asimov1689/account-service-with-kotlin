package account.controller

import account.dto.RoleChangeRequest
import account.dto.UserAdminResponse
import account.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(private val adminService: AdminService) {

    @GetMapping("/user", "/user/")
    fun getAllUsers(): ResponseEntity<List<UserAdminResponse>> =
        ResponseEntity.ok(adminService.getAllUsers())

    @DeleteMapping("/user/{email}")
    fun deleteUser(
        @PathVariable email: String
    ): ResponseEntity<Map<String, String>> {
        adminService.deleteUser(email)
        return ResponseEntity.ok(
            mapOf(
                "user" to email.lowercase(),
                "status" to "Deleted successfully!"
            )
        )
    }

    @PutMapping("/user/role")
    fun changeRole(
        @RequestBody request: RoleChangeRequest
    ): ResponseEntity<UserAdminResponse> =
        ResponseEntity.ok(adminService.changeRole(request))
}