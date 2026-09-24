package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.security.UserAccessManagementService;
import org.immregistries.iis.kernal.security.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administration of every user account.
 * <p>
 * Access is enforced by the {@code /rest/admin/**} matcher in ServerSecurityConfig, which
 * requires ROLE_ADMIN and is registered ahead of the blanket {@code /rest/**} permitAll.
 */
@RestController
@RequestMapping(IisRestPath.REST_ADMIN_USERS_PATH)
public class AdminUserRestController {

	@Autowired
	private UserAccessManagementService userAccessManagementService;

	@GetMapping
	public List<AdminUserSummaryDto> getAllUsers() {
		return userAccessManagementService.listAllUsers();
	}

	@PostMapping
	public AdminUserSummaryDto createUser(@RequestBody CreateUserRequest request) {
		return userAccessManagementService.createUser(request);
	}

	@GetMapping(IisPathVariable.PlaceHolder.USER_ID_PLACEHOLDER)
	public AdminUserSummaryDto getUser(@PathVariable(IisPathVariable.Key.USER_ID) int userId) {
		return userAccessManagementService.getUser(userId);
	}

	/**
	 * The target user keeps any session they already hold, but its cached copy of the old
	 * hash is now stale, so their internal FHIR calls fail until they sign in again.
	 */
	@PutMapping(IisPathVariable.PlaceHolder.USER_ID_PLACEHOLDER + IisRestPath.BasePath.PASSWORD_PATH)
	public ResponseEntity<Void> setPassword(@PathVariable(IisPathVariable.Key.USER_ID) int userId,
	                                        @RequestBody SetPasswordRequest request) {
		userAccessManagementService.setPassword(userId, request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@PutMapping(IisPathVariable.PlaceHolder.USER_ID_PLACEHOLDER + IisRestPath.BasePath.ENABLED_PATH)
	public AdminUserSummaryDto setEnabled(@PathVariable(IisPathVariable.Key.USER_ID) int userId,
	                                      @RequestBody SetEnabledRequest request) {
		return userAccessManagementService.setEnabled(userId, request.isEnabled());
	}

	@PutMapping(IisPathVariable.PlaceHolder.USER_ID_PLACEHOLDER + IisRestPath.BasePath.ROLES_PATH)
	public AdminUserSummaryDto setRoles(@PathVariable(IisPathVariable.Key.USER_ID) int userId,
	                                    @RequestBody SetRolesRequest request) {
		return userAccessManagementService.setRoles(userId, request.getRoles());
	}

	@PutMapping(IisPathVariable.PlaceHolder.USER_ID_PLACEHOLDER + IisRestPath.BasePath.EMAIL_PATH)
	public AdminUserSummaryDto setEmail(@PathVariable(IisPathVariable.Key.USER_ID) int userId,
	                                    @RequestBody UpdateEmailRequest request) {
		return userAccessManagementService.setEmail(userId, request.getEmail());
	}
}
