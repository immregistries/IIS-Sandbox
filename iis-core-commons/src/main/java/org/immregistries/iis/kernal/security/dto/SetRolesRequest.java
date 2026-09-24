package org.immregistries.iis.kernal.security.dto;

import org.immregistries.iis.kernal.enums.UserRole;

import java.util.Set;

public class SetRolesRequest {

	private Set<UserRole> roles;

	public SetRolesRequest() {
	}

	public SetRolesRequest(Set<UserRole> roles) {
		this.roles = roles;
	}

	public Set<UserRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<UserRole> roles) {
		this.roles = roles;
	}
}
