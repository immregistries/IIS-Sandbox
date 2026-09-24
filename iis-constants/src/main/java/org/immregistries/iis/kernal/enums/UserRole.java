package org.immregistries.iis.kernal.enums;

/**
 * Privileges held by a UserAccess.
 * <p>
 * The {@code ROLE_} prefix is a Spring Security convention: {@code hasRole("ADMIN")}
 * prepends it before comparing against the granted authorities, so {@link #getAuthority()}
 * must stay in sync with the constant name.
 * <p>
 * This module has no dependencies, so the authority is exposed as a plain String and
 * wrapped into a GrantedAuthority by UserAccess.
 */
public enum UserRole {
	USER("ROLE_USER"),
	ADMIN("ROLE_ADMIN");

	public static final String ROLE_PREFIX = "ROLE_";

	private final String authority;

	UserRole(String authority) {
		this.authority = authority;
	}

	public String getAuthority() {
		return authority;
	}
}
