package org.immregistries.iis.kernal.controllers.rest;

import java.util.List;

public class AuthInfo {

	private boolean authenticated;
	private String name;
	private Object principal;
	private List<Authority> authorities;

	public AuthInfo() {
	}

	public AuthInfo(boolean authenticated, String name, Object principal, List<Authority> authorities) {
		this.authenticated = authenticated;
		this.name = name;
		this.principal = principal;
		this.authorities = authorities;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getPrincipal() {
		return principal;
	}

	public void setPrincipal(Object principal) {
		this.principal = principal;
	}

	public List<Authority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(List<Authority> authorities) {
		this.authorities = authorities;
	}

	public static class Authority {

		private String authority;

		public Authority() {
		}

		public Authority(String authority) {
			this.authority = authority;
		}

		public String getAuthority() {
			return authority;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}
	}
}
