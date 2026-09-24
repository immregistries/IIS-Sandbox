package org.immregistries.iis.kernal.security.dto;

public class SetPasswordRequest {

	private String newPassword;

	public SetPasswordRequest() {
	}

	public SetPasswordRequest(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}
