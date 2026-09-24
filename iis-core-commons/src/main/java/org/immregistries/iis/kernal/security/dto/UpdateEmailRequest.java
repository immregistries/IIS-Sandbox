package org.immregistries.iis.kernal.security.dto;

public class UpdateEmailRequest {

	private String email;

	public UpdateEmailRequest() {
	}

	public UpdateEmailRequest(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
