package account.controller

import account.dto.RoleChangeRequest
import account.dto.StatusResponse
import account.dto.UserAccessRequest
import account.dto.UserAdminResponse
import account.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
) {
    @GetMapping("/user", "/user/")
    fun getAllUsers(): ResponseEntity<List<UserAdminResponse>> = ResponseEntity.ok(adminService.getAllUsers())

    @DeleteMapping("/user/{email}")
    fun deleteUser(
        @PathVariable email: String,
        @AuthenticationPrincipal admin: UserDetails,
    ): ResponseEntity<Map<String, String>> = ResponseEntity.ok(adminService.deleteUser(email, admin.username))

    @PutMapping("/user/role")
    fun changeRole(
        @RequestBody request: RoleChangeRequest,
        @AuthenticationPrincipal admin: UserDetails,
    ): ResponseEntity<UserAdminResponse> = ResponseEntity.ok(adminService.changeRole(request, admin.username))

    @PutMapping("/user/access")
    fun changeAccess(
        @RequestBody request: UserAccessRequest,
        @AuthenticationPrincipal admin: UserDetails,
    ): ResponseEntity<StatusResponse> = ResponseEntity.ok(adminService.changeAccess(request, admin.username))
}
