package org.immregistries.iis.kernal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents the JSON structure for ShLinkManifestRequestBody.
 * This class is ready for JSON serialization using Jackson.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"recipient",
	"passcode",
	"embeddedLengthMax"
})
public class ShLinkManifestRequestBody {

	/**
	 * A string describing the recipient (e.g., the name of an organization or person)
	 * suitable for display to the Receiving User. This field is mandatory.
	 */
	@JsonProperty(value = "recipient", required = true)
	private String recipient;

	/**
	 * SHALL be populated with a user-supplied Passcode if the 'P' flag was present
	 * in the SMART Health Link payload. This field is optional.
	 */
	@JsonProperty("passcode")
	private String passcode;

	/**
	 * Integer upper bound on the length of embedded payloads (see .files.embedded).
	 * This field is optional.
	 */
	@JsonProperty("embeddedLengthMax")
	private Integer embeddedLengthMax;

	public ShLinkManifestRequestBody() {
	}

	public ShLinkManifestRequestBody(String recipient, String passcode, Integer embeddedLengthMax) {
		this.recipient = recipient;
		this.passcode = passcode;
		this.embeddedLengthMax = embeddedLengthMax;
	}

	// Getters and Setters

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getPasscode() {
		return passcode;
	}

	public void setPasscode(String passcode) {
		this.passcode = passcode;
	}

	public Integer getEmbeddedLengthMax() {
		return embeddedLengthMax;
	}

	public void setEmbeddedLengthMax(Integer embeddedLengthMax) {
		this.embeddedLengthMax = embeddedLengthMax;
	}
}
