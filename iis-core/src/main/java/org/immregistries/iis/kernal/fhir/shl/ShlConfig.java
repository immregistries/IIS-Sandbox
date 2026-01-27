package org.immregistries.iis.kernal.fhir.shl;

import ca.uhn.fhir.model.api.IResource;

public class ShlConfig {

//	What to share. Depending on the SMART Health Links Sharing Application, the Sharing User might explicitly choose a set of files or define a “sharing policy” that matches different data over time.
	public String sharing(IResource resource) {
		return ""; // Return IPS?
	}
//	Whether the SMART Health Links will require a Passcode to access. Depending on the SMART Health Links Sharing Application, a Passcode may be mandatory.
	public boolean requirePasscode() {
		return  false;
	}
//	Whether the SMART Health Links will expire at some pre-specified time. Depending on the SMART Health Links Sharing Application, an expiration time may be mandatory.

}
