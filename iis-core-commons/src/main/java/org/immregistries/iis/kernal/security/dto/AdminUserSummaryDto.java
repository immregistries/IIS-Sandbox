package org.immregistries.iis.kernal.security.dto;

import java.util.List;
import java.util.Set;

/**
 * One row of the admin user listing: the user plus the tenants they own.
 */
public class AdminUserSummaryDto {

	private int userAccessId;
	private String accessName;
	private String email;
	private boolean enabled;
	private Set<String> roles;
	private List<String> tenantNames;

	public AdminUserSummaryDto() {
	}

	public AdminUserSummaryDto(int userAccessId, String accessName, String email, boolean enabled, Set<String> roles,
	                           List<String> tenantNames) {
		this.userAccessId = userAccessId;
		this.accessName = accessName;
		this.email = email;
		this.enabled = enabled;
		this.roles = roles;
		this.tenantNames = tenantNames;
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

	public List<String> getTenantNames() {
		return tenantNames;
	}

	public void setTenantNames(List<String> tenantNames) {
		this.tenantNames = tenantNames;
	}
}
