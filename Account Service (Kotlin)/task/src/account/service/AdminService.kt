package account.service

import account.dto.RoleChangeRequest
import account.dto.UserAdminResponse
import account.entity.toAdminResponse
import account.exception.AdminOperationException
import account.exception.UserNotFoundException
import account.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val userService: UserService
) {

    companion object {
        private val ADMIN_ROLES = setOf("ROLE_ADMINISTRATOR")
        private val BUSINESS_ROLES = setOf("ROLE_USER", "ROLE_ACCOUNTANT")
        private val ALL_ROLES = ADMIN_ROLES + BUSINESS_ROLES
    }

    fun getAllUsers(): List<UserAdminResponse> =
        userRepository.findAll()
            .sortedBy { it.id }
            .map { it.toAdminResponse() }

    @Transactional
    fun deleteUser(email: String) {
        val target = userRepository.findByEmail(email.lowercase())
            ?: throw UserNotFoundException()

        if (target.roles.any { it.name in ADMIN_ROLES }) {
            throw AdminOperationException("Can't remove ADMINISTRATOR role!")
        }

        userRepository.delete(target)
    }

    @Transactional
    fun changeRole(request: RoleChangeRequest): UserAdminResponse {
        val user = userRepository.findByEmail(request.user.lowercase())
            ?: throw UserNotFoundException()

        val roleName = "ROLE_${request.role.uppercase()}"
        if (roleName !in ALL_ROLES) throw UserNotFoundException("Role not found!")
        val role = userService.getOrCreateRole(roleName)

        when (request.operation.uppercase()) {
            "GRANT" -> {
                val userIsAdmin = user.roles.any { it.name in ADMIN_ROLES }
                val userIsBusiness = user.roles.any { it.name in BUSINESS_ROLES }
                val grantingAdmin = roleName in ADMIN_ROLES
                val grantingBusiness = roleName in BUSINESS_ROLES

                if ((userIsAdmin && grantingBusiness) || (userIsBusiness && grantingAdmin)) {
                    throw AdminOperationException(
                        "The user cannot combine administrative and business roles!"
                    )
                }
                user.roles.add(role)
            }
            "REMOVE" -> {
                if (roleName in ADMIN_ROLES) {
                    throw AdminOperationException("Can't remove ADMINISTRATOR role!")
                }
                if (user.roles.none { it.name == roleName }) {
                    throw AdminOperationException("The user does not have a role!")
                }
                if (user.roles.size == 1) {
                    throw AdminOperationException("The user must have at least one role!")
                }
                user.roles.removeIf { it.name == roleName }
            }
            else -> throw AdminOperationException("Invalid operation!")
        }

        return userRepository.save(user).toAdminResponse()
    }
}