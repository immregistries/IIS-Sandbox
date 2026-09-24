package org.immregistries.iis.kernal.security.dto;

import org.immregistries.iis.kernal.enums.UserRole;

import java.util.Set;

public class CreateUserRequest {

	private String accessName;
	private String password;
	private String email;
	private Set<UserRole> roles;

	public CreateUserRequest() {
	}

	public CreateUserRequest(String accessName, String password, String email, Set<UserRole> roles) {
		this.accessName = accessName;
		this.password = password;
		this.email = email;
		this.roles = roles;
	}

	public String getAccessName() {
		return accessName;
	}

	public void setAccessName(String accessName) {
		this.accessName = accessName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<UserRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<UserRole> roles) {
		this.roles = roles;
	}
}
