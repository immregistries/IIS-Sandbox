package org.immregistries.iis.kernal.security.dto;

public class SetEnabledRequest {

	private boolean enabled;

	public SetEnabledRequest() {
	}

	public SetEnabledRequest(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
