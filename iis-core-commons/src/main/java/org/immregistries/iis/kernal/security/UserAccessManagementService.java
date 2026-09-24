package org.immregistries.iis.kernal.security;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.enums.UserRole;
import org.immregistries.iis.kernal.persisted.SecondDatabaseConfiguration;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.persisted.repository.UserAccessRepository;
import org.immregistries.iis.kernal.security.dto.AdminUserSummaryDto;
import org.immregistries.iis.kernal.security.dto.CreateUserRequest;
import org.immregistries.iis.kernal.security.dto.UserProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User self-management and administration.
 * <p>
 * Every operation reloads the user from the database rather than trusting the passed-in
 * principal: that object is a detached snapshot taken at login, cached in the HttpSession,
 * so its enabled flag, roles and password hash may all be stale. The reload is also where
 * a disabled account is caught for callers that already hold a session.
 * <p>
 * These entities live on the second datasource, so every transaction names its manager
 * explicitly.
 */
@Service
public class UserAccessManagementService {

	public static final int MIN_PASSWORD_LENGTH = 8;

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");

	@Autowired
	private UserAccessRepository userAccessRepository;
	@Autowired
	private TenantRepository tenantRepository;

	/*
	 * Self service
	 */

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER, readOnly = true)
	public UserProfileDto getOwnProfile(UserAccess principal) {
		return toProfile(requireActive(principal));
	}

	/**
	 * @return the saved entity, so the caller can replace the stale copy held in the
	 * SecurityContext and the session. That matters because IisFhirClientFactory sends the
	 * stored hash as the Basic-auth password for internal FHIR calls.
	 */
	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public UserAccess changeOwnPassword(UserAccess principal, String currentPassword, String newPassword) {
		UserAccess userAccess = requireActive(principal);
		if (StringUtils.isBlank(currentPassword)) {
			throw new AuthenticationException("Current password is required");
		}
		/*
		 * checkpw only, never the plaintext equals fallback used at login: the stored hash is
		 * itself an accepted credential there, so honouring it here would turn knowledge of
		 * the hash into account takeover.
		 */
		if (!BCrypt.checkpw(currentPassword, userAccess.getAccessKey())) {
			throw new AuthenticationException("Current password does not match");
		}
		validateNewPassword(newPassword);
		if (BCrypt.checkpw(newPassword, userAccess.getAccessKey())) {
			throw new IllegalArgumentException("New password must differ from the current one");
		}
		userAccess.setAccessKey(UserAccessUtil.hashPassword(newPassword));
		return userAccessRepository.save(userAccess);
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public UserProfileDto updateOwnEmail(UserAccess principal, String email) {
		UserAccess userAccess = requireActive(principal);
		userAccess.setEmail(normaliseEmail(email));
		return toProfile(userAccessRepository.save(userAccess));
	}

	/*
	 * Administration
	 */

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER, readOnly = true)
	public List<AdminUserSummaryDto> listAllUsers() {
		Map<Integer, List<String>> tenantsByUser = tenantsByUserAccessId();
		return userAccessRepository.findAllWithRoles().stream()
			.map(userAccess -> toAdminSummary(userAccess, tenantsByUser))
			.sorted((a, b) -> a.getAccessName().compareToIgnoreCase(b.getAccessName()))
			.collect(Collectors.toList());
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER, readOnly = true)
	public AdminUserSummaryDto getUser(int userAccessId) {
		return toAdminSummary(require(userAccessId), tenantsByUserAccessId());
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public AdminUserSummaryDto createUser(CreateUserRequest request) {
		String accessName = StringUtils.trimToEmpty(request.getAccessName());
		if (StringUtils.isBlank(accessName)) {
			throw new IllegalArgumentException("accessName is required");
		}
		if (accessName.startsWith(UserAccessUtil.GITHUB_PREFIX)) {
			throw new IllegalArgumentException(
				"accessName must not start with " + UserAccessUtil.GITHUB_PREFIX + ", those are created by OAuth login");
		}
		if (userAccessRepository.findByAccessNameIgnoreCase(accessName).isPresent()) {
			throw new IllegalArgumentException("A user named " + accessName + " already exists");
		}
		validateNewPassword(request.getPassword());

		UserAccess userAccess = new UserAccess();
		userAccess.setAccessName(accessName);
		userAccess.setAccessKey(UserAccessUtil.hashPassword(request.getPassword()));
		userAccess.setEmail(normaliseEmail(request.getEmail()));
		userAccess.setEnabled(true);
		userAccess.setRoles(request.getRoles() == null || request.getRoles().isEmpty()
			? EnumSet.of(UserRole.USER)
			: new HashSet<>(request.getRoles()));
		return toAdminSummary(userAccessRepository.save(userAccess), tenantsByUserAccessId());
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public void setPassword(int userAccessId, String newPassword) {
		UserAccess userAccess = require(userAccessId);
		validateNewPassword(newPassword);
		userAccess.setAccessKey(UserAccessUtil.hashPassword(newPassword));
		userAccessRepository.save(userAccess);
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public AdminUserSummaryDto setEnabled(int userAccessId, boolean enabled) {
		UserAccess userAccess = require(userAccessId);
		if (!enabled) {
			assertNotLastEnabledAdmin(userAccess, "disable");
		}
		userAccess.setEnabled(enabled);
		return toAdminSummary(userAccessRepository.save(userAccess), tenantsByUserAccessId());
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public AdminUserSummaryDto setRoles(int userAccessId, Set<UserRole> roles) {
		UserAccess userAccess = require(userAccessId);
		Set<UserRole> target = roles == null || roles.isEmpty() ? EnumSet.of(UserRole.USER) : new HashSet<>(roles);
		if (!target.contains(UserRole.ADMIN)) {
			assertNotLastEnabledAdmin(userAccess, "revoke ADMIN from");
		}
		userAccess.setRoles(target);
		return toAdminSummary(userAccessRepository.save(userAccess), tenantsByUserAccessId());
	}

	@Transactional(transactionManager = SecondDatabaseConfiguration.IIS_LOCAL_TRANSACTION_MANAGER)
	public AdminUserSummaryDto setEmail(int userAccessId, String email) {
		UserAccess userAccess = require(userAccessId);
		userAccess.setEmail(normaliseEmail(email));
		return toAdminSummary(userAccessRepository.save(userAccess), tenantsByUserAccessId());
	}

	/*
	 * Internals
	 */

	private UserAccess require(int userAccessId) {
		return userAccessRepository.findById(userAccessId)
			.orElseThrow(() -> new UserNotFoundException(userAccessId));
	}

	/**
	 * Reloads the principal and rejects it if it has since been disabled. This is what makes
	 * a disable take effect for a user who already holds a session.
	 */
	private UserAccess requireActive(UserAccess principal) {
		if (principal == null) {
			throw new AuthenticationException("Not authenticated");
		}
		UserAccess userAccess = require(principal.getUserAccessId());
		if (!userAccess.isEnabled()) {
			throw new AuthenticationException("user disabled : " + userAccess.getAccessName());
		}
		return userAccess;
	}

	/**
	 * Stops an administrator locking every administrator out of the deployment.
	 */
	private void assertNotLastEnabledAdmin(UserAccess userAccess, String action) {
		if (userAccess.hasRole(UserRole.ADMIN) && userAccess.isEnabled()
			&& userAccessRepository.countEnabledWithRole(UserRole.ADMIN) <= 1) {
			throw new IllegalArgumentException(
				"Cannot " + action + " " + userAccess.getAccessName() + ": it is the last enabled ADMIN");
		}
	}

	private void validateNewPassword(String newPassword) {
		if (StringUtils.isBlank(newPassword)) {
			throw new IllegalArgumentException("New password is required");
		}
		if (newPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
	}

	/**
	 * @return null for a blank value, so a user can clear their email.
	 */
	private String normaliseEmail(String email) {
		if (StringUtils.isBlank(email)) {
			return null;
		}
		String normalised = email.trim().toLowerCase(Locale.ROOT);
		if (!EMAIL_PATTERN.matcher(normalised).matches()) {
			throw new IllegalArgumentException("Not a valid email address : " + email);
		}
		return normalised;
	}

	private Map<Integer, List<String>> tenantsByUserAccessId() {
		return tenantRepository.findAllTenantOwners().stream()
			.filter(view -> view.getUserAccessId() != null)
			.collect(Collectors.groupingBy(TenantRepository.TenantOwnerView::getUserAccessId,
				Collectors.mapping(TenantRepository.TenantOwnerView::getOrganizationName, Collectors.toList())));
	}

	private Set<String> roleNames(UserAccess userAccess) {
		return userAccess.getEffectiveRoles().stream().map(Enum::name).collect(Collectors.toSet());
	}

	private UserProfileDto toProfile(UserAccess userAccess) {
		return new UserProfileDto(userAccess.getUserAccessId(), userAccess.getAccessName(), userAccess.getEmail(),
			userAccess.isEnabled(), roleNames(userAccess));
	}

	private AdminUserSummaryDto toAdminSummary(UserAccess userAccess, Map<Integer, List<String>> tenantsByUser) {
		List<String> tenantNames =
			new ArrayList<>(tenantsByUser.getOrDefault(userAccess.getUserAccessId(), List.of()));
		tenantNames.sort(String::compareToIgnoreCase);
		return new AdminUserSummaryDto(userAccess.getUserAccessId(), userAccess.getAccessName(), userAccess.getEmail(),
			userAccess.isEnabled(), roleNames(userAccess), tenantNames);
	}
}
