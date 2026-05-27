package account.service

import account.dto.RoleChangeRequest
import account.dto.StatusResponse
import account.dto.UserAccessRequest
import account.dto.UserAdminResponse
import account.entity.toAdminResponse
import account.exception.AdminOperationException
import account.exception.UserNotFoundException
import account.repository.RoleRepository
import account.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userService: UserService,
    private val securityEventService: SecurityEventService,
) {
    companion object {
        private val ADMIN_ROLES = setOf("ROLE_ADMINISTRATOR")
        private val BUSINESS_ROLES = setOf("ROLE_USER", "ROLE_ACCOUNTANT", "ROLE_AUDITOR")
        private val ALL_ROLES = ADMIN_ROLES + BUSINESS_ROLES
    }

    fun getAllUsers(): List<UserAdminResponse> =
        userRepository
            .findAll()
            .sortedBy { it.id }
            .map { it.toAdminResponse() }

    fun deleteUser(
        email: String,
        adminEmail: String,
    ): Map<String, String> {
        val user =
            userRepository.findByEmail(email.lowercase())
                ?: throw UserNotFoundException()

        if (user.roles.any { it.name == "ROLE_ADMINISTRATOR" }) {
            throw AdminOperationException("Can't remove ADMINISTRATOR role!")
        }

        userRepository.delete(user)

        securityEventService.log(
            action = "DELETE_USER",
            subject = adminEmail,
            obj = email,
            path = "/api/admin/user",
        )

        return mapOf("user" to email, "status" to "Deleted successfully!")
    }

    fun changeRole(
        request: RoleChangeRequest,
        adminEmail: String,
    ): UserAdminResponse {
        val email = request.user.lowercase()
        val roleName = "ROLE_${request.role.uppercase()}"

        if (roleName !in ALL_ROLES) {
            throw AdminOperationException("Role not found!")
        }

        val user =
            userRepository.findByEmail(email)
                ?: throw UserNotFoundException()

        when (request.operation.uppercase()) {
            "GRANT" -> {
                val isAdminRole = roleName in ADMIN_ROLES
                val isBusinessRole = roleName in BUSINESS_ROLES

                val userHasAdmin = user.roles.any { it.name in ADMIN_ROLES }
                val userHasBusiness = user.roles.any { it.name in BUSINESS_ROLES }

                if ((isAdminRole && userHasBusiness) || (isBusinessRole && userHasAdmin)) {
                    throw AdminOperationException(
                        "The user cannot combine administrative and business roles!",
                    )
                }
                if (user.roles.any { it.name == roleName }) {
                    throw AdminOperationException("The user already has this role!")
                }

                val role = userService.getOrCreateRole(roleName)
                user.roles.add(role)
                userRepository.save(user)

                securityEventService.log(
                    action = "GRANT_ROLE",
                    subject = adminEmail,
                    obj = "Grant role ${request.role.uppercase()} to ${user.email}",
                    path = "/api/admin/user/role",
                )
            }

            "REMOVE" -> {
                if (roleName == "ROLE_ADMINISTRATOR") {
                    throw AdminOperationException("Can't remove ADMINISTRATOR role!")
                }
                if (user.roles.none { it.name == roleName }) {
                    throw AdminOperationException("The user does not have a role!")
                }
                if (user.roles.size == 1) {
                    throw AdminOperationException("The user must have at least one role!")
                }

                user.roles.removeIf { it.name == roleName }
                userRepository.save(user)

                securityEventService.log(
                    action = "REMOVE_ROLE",
                    subject = adminEmail,
                    obj = "Remove role ${request.role.uppercase()} from ${user.email}",
                    path = "/api/admin/user/role",
                )
            }

            else -> {
                throw AdminOperationException("Unknown operation: ${request.operation}")
            }
        }

        return userRepository.findByEmail(email)!!.toAdminResponse()
    }

    fun changeAccess(
        request: UserAccessRequest,
        adminEmail: String,
    ): StatusResponse {
        val email =
            request.user?.lowercase()
                ?: throw AdminOperationException("User field must not be empty")
        val operation =
            request.operation?.uppercase()
                ?: throw AdminOperationException("Operation field must not be empty")

        val user =
            userRepository.findByEmail(email)
                ?: throw UserNotFoundException()

        if (user.roles.any { it.name == "ROLE_ADMINISTRATOR" }) {
            throw AdminOperationException("Can't lock the ADMINISTRATOR!")
        }

        return when (operation) {
            "LOCK" -> {
                userService.lockUser(email)

                securityEventService.log(
                    action = "LOCK_USER",
                    subject = adminEmail,
                    obj = "Lock user $email",
                    path = "/api/admin/user/access",
                )

                StatusResponse("User $email locked!")
            }

            "UNLOCK" -> {
                userService.unlockUser(email)

                securityEventService.log(
                    action = "UNLOCK_USER",
                    subject = adminEmail,
                    obj = "Unlock user $email",
                    path = "/api/admin/user/access",
                )

                StatusResponse("User $email unlocked!")
            }

            else -> {
                throw AdminOperationException("Invalid operation: $operation")
            }
        }
    }
}
