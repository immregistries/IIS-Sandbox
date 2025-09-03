package org.immregistries.iis.kernal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java class representing the payload structure for a Smart Health Link File.
 * This class is designed to be serialized from and deserialized to JSON.
 */
public class ShLinkFilePayload {

	@JsonProperty("type")
	private List<String> type = new ArrayList<>();

	@JsonProperty("verifiableCredential")
	private List<String> verifiableCredential = new ArrayList<>();

	/**
	 * Default no-argument constructor. Required for JSON deserialization.
	 */
	public ShLinkFilePayload() {
	}

	/**
	 * Constructs a new ShLinkFilePayload with the given type and verifiable credential.
	 *
	 * @param type                 The list of types.
	 * @param verifiableCredential The list of verifiable credentials.
	 */
	public ShLinkFilePayload(List<String> type, List<String> verifiableCredential) {
		this.type = type;
		this.verifiableCredential = verifiableCredential;
	}

	// Getters and Setters

	public List<String> getType() {
		return type;
	}

	public void setType(List<String> type) {
		this.type = type;
	}

	public List<String> getVerifiableCredential() {
		return verifiableCredential;
	}

	public void setVerifiableCredential(List<String> verifiableCredential) {
		this.verifiableCredential = verifiableCredential;
	}

	@Override
	public String toString() {
		return "SmartHealthLinkFilePayload{" +
			"type=" + type +
			", verifiableCredential=" + verifiableCredential +
			'}';
	}
}
