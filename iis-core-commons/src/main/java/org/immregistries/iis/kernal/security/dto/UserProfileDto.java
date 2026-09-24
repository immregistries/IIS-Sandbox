package org.immregistries.iis.kernal.security.dto;

import java.util.Set;

/**
 * What a user may see about themselves. Never exposes accessKey.
 * Roles are the plain enum names ("ADMIN"); the ROLE_ prefix stays an internal Spring detail.
 */
public class UserProfileDto {

	private int userAccessId;
	private String accessName;
	private String email;
	private boolean enabled;
	private Set<String> roles;

	public UserProfileDto() {
	}

	public UserProfileDto(int userAccessId, String accessName, String email, boolean enabled, Set<String> roles) {
		this.userAccessId = userAccessId;
		this.accessName = accessName;
		this.email = email;
		this.enabled = enabled;
		this.roles = roles;
	}

	public int getUserAccessId() {
		return userAccessId;
	}

	public void setUserAccessId(int userAccessId) {
		this.userAccessId = userAccessId;
	}

	public String getAccessName() {
		return accessName;
	}

	public void setAccessName(String accessName) {
		this.accessName = accessName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
}
