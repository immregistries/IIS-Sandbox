package org.immregistries.iis.kernal.security;

import org.immregistries.iis.kernal.enums.UserRole;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backward compatibility and authority mapping on UserAccess. A user row created before the
 * roles and enabled columns existed reads back with no roles and a null enabled flag, and
 * must keep behaving as an ordinary, active user.
 */
class UserAccessRolesTest {

	private List<String> authoritiesOf(UserAccess userAccess) {
		return userAccess.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toList());
	}

	@Test
	void legacyUserWithoutRolesIsAPlainUser() {
		UserAccess userAccess = new UserAccess();
		assertEquals(List.of("ROLE_USER"), authoritiesOf(userAccess));
	}

	@Test
	void nullRolesSetIsToleratedAsAPlainUser() {
		UserAccess userAccess = new UserAccess();
		userAccess.setRoles(null);
		assertEquals(List.of("ROLE_USER"), authoritiesOf(userAccess));
	}

	@Test
	void adminRoleMapsToRoleAdminAuthority() {
		UserAccess userAccess = new UserAccess();
		userAccess.setRoles(new HashSet<>(List.of(UserRole.ADMIN)));
		assertEquals(List.of("ROLE_ADMIN"), authoritiesOf(userAccess));
		assertTrue(userAccess.hasRole(UserRole.ADMIN));
		assertFalse(userAccess.hasRole(UserRole.USER));
	}

	/**
	 * hasRole("ADMIN") in ServerSecurityConfig prepends ROLE_ before comparing, so the two
	 * must stay in sync or every admin request silently 403s.
	 */
	@Test
	void authorityIsTheRolePrefixPlusTheConstantName() {
		for (UserRole role : UserRole.values()) {
			assertEquals(UserRole.ROLE_PREFIX + role.name(), role.getAuthority());
		}
	}

	@Test
	void nullEnabledMeansEnabled() {
		UserAccess userAccess = new UserAccess();
		assertTrue(userAccess.isEnabled(), "a row predating the enabled column must count as enabled");
	}

	@Test
	void onlyAnExplicitFalseDisables() {
		UserAccess userAccess = new UserAccess();
		userAccess.setEnabled(Boolean.TRUE);
		assertTrue(userAccess.isEnabled());
		userAccess.setEnabled(Boolean.FALSE);
		assertFalse(userAccess.isEnabled());
	}

	/**
	 * TenantRestController compares the tenant owner against the principal, which are two
	 * different instances of the same row.
	 */
	@Test
	void equalityIsByIdNotIdentity() {
		UserAccess one = new UserAccess();
		one.setUserAccessId(7);
		one.setAccessName("Mercy");
		UserAccess sameRowOtherInstance = new UserAccess();
		sameRowOtherInstance.setUserAccessId(7);
		sameRowOtherInstance.setAccessName("stale-name");
		UserAccess other = new UserAccess();
		other.setUserAccessId(8);

		assertEquals(one, sameRowOtherInstance);
		assertEquals(one.hashCode(), sameRowOtherInstance.hashCode());
		assertNotEquals(one, other);
	}

	@Test
	void isAuthenticatedStaysTrueForADisabledUser() {
		UserAccess userAccess = new UserAccess();
		userAccess.setEnabled(Boolean.FALSE);
		/*
		 * Returning false here would make ExceptionTranslationFilter treat the caller as
		 * anonymous, wipe their session and redirect to the login page instead of answering
		 * with a status code. Disabled accounts are rejected at authentication time and
		 * re-checked against the database in UserAccessManagementService.
		 */
		assertTrue(userAccess.isAuthenticated());
	}
}
