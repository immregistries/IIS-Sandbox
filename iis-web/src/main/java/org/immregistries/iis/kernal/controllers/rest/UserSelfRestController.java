package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.UserAccessManagementService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.immregistries.iis.kernal.security.dto.ChangePasswordRequest;
import org.immregistries.iis.kernal.security.dto.UpdateEmailRequest;
import org.immregistries.iis.kernal.security.dto.UserProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Lets a signed-in user manage their own account.
 * <p>
 * Access is enforced by the {@code /rest/user/**} matcher in ServerSecurityConfig, which is
 * registered ahead of the blanket {@code /rest/**} permitAll.
 */
@RestController
@RequestMapping(IisRestPath.REST_USER_PATH + IisRestPath.BasePath.ME_PATH)
public class UserSelfRestController {

	@Autowired
	private UserAccessManagementService userAccessManagementService;

	@GetMapping
	public UserProfileDto getOwnProfile(@AuthenticationPrincipal UserAccess userAccess) {
		return userAccessManagementService.getOwnProfile(userAccess);
	}

	/**
	 * Changing the password rotates the stored hash, and that hash doubles as the credential
	 * IisFhirClientFactory sends on internal FHIR calls. The cached copies in the
	 * SecurityContext and the session must therefore be replaced, or the rest of this
	 * session would keep presenting the old one.
	 */
	@PostMapping(IisRestPath.BasePath.PASSWORD_PATH)
	public ResponseEntity<Void> changeOwnPassword(@AuthenticationPrincipal UserAccess userAccess,
	                                              @RequestBody ChangePasswordRequest request, HttpServletRequest httpServletRequest) {
		UserAccess updated = userAccessManagementService.changeOwnPassword(userAccess, request.getCurrentPassword(),
			request.getNewPassword());
		SecurityContextHolder.getContext().setAuthentication(updated);
		HttpSession session = httpServletRequest.getSession(false);
		if (session != null) {
			session.setAttribute(UserAccessUtil.SESSION_USER_ACCESS, updated);
		}
		return ResponseEntity.noContent().build();
	}

	@PutMapping(IisRestPath.BasePath.EMAIL_PATH)
	public UserProfileDto updateOwnEmail(@AuthenticationPrincipal UserAccess userAccess,
	                                     @RequestBody UpdateEmailRequest request) {
		return userAccessManagementService.updateOwnEmail(userAccess, request.getEmail());
	}
}
