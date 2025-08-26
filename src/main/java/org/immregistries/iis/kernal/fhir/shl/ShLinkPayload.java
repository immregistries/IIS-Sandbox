package org.immregistries.iis.kernal.fhir.shl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;
import java.util.Set;

/**
 * Represents a SMART Health Link, which provides a link to a user's health information.
 */
public class ShLinkPayload {

	/**
	 * The URL for this SMART Health Link.
	 */
	@JsonProperty(value = "url", required = true)
	private String url;

	/**
	 * Decryption key for processing files returned in the manifest.
	 * 43 characters, consisting of 32 random bytes base64urlencoded.
	 */
	@JsonProperty("key")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String key;

	/**
	 * Number representing expiration time in Epoch seconds.
	 * A hint to help the SMART Health Links Receiving Application determine if this QR is stale.
	 * (Note: epoch times should be parsed into 64-bit numeric types.)
	 */
	@JsonProperty("exp")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long exp;

	/**
	 * String created by concatenating single-character flags in alphabetical order.
	 *
	 * <ul>
	 * <li>L: Indicates the SMART Health Link is intended for long-term use and manifest content can evolve over time.</li>
	 * <li>P: Indicates the SMART Health Link requires a Passcode to resolve.</li>
	 * <li>U: Indicates the SMART Health Links's `url` resolves to a single encrypted file accessible via `GET`, bypassing the manifest. SHALL NOT be used in combination with P.</li>
	 * </ul>
	 */
	@JsonProperty("flag")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String flag;

	/**
	 * A short description of the data behind the SMART Health Link, no longer than 80 characters.
	 */
	@JsonProperty("label")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String label;

	/**
	 * Integer representing the SMART Health Links protocol version this SMART Health Link conforms to.
	 * MAY be omitted when the default value (1) applies.
	 */
	@JsonProperty("v")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer v = 1;

	// Getters and Setters

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Optional<String> getKey() {
		return Optional.ofNullable(key);
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Optional<Long> getExp() {
		return Optional.ofNullable(exp);
	}

	public void setExp(Long exp) {
		this.exp = exp;
	}

	public Optional<String> getFlag() {
		return Optional.ofNullable(flag);
	}

	public void setFlag(String flag) {
		// Optional validation to ensure flag characters are valid and in order.
		if (flag != null) {
			Set<Character> validFlags = Set.of('L', 'P', 'U');
			for (char c : flag.toCharArray()) {
				if (!validFlags.contains(c)) {
					throw new IllegalArgumentException("Invalid flag character: " + c);
				}
			}
			if (flag.contains("P") && flag.contains("U")) {
				throw new IllegalArgumentException("Flag 'P' and 'U' cannot be used together.");
			}
			// Additional check for alphabetical order
		}
		this.flag = flag;
	}

	public Optional<String> getLabel() {
		return Optional.ofNullable(label);
	}

	public void setLabel(String label) {
		if (label != null && label.length() > 80) {
			throw new IllegalArgumentException("Label cannot be longer than 80 characters.");
		}
		this.label = label;
	}

	public Optional<Integer> getV() {
		return Optional.ofNullable(v);
	}

	public void setV(Integer v) {
		this.v = v;
	}

	@Override
	public String toString() {
		return "ShLinkPayload{" +
			"url='" + url + '\'' +
			", key='" + key + '\'' +
			", exp=" + exp +
			", flag='" + flag + '\'' +
			", label='" + label + '\'' +
			", v=" + v +
			'}';
	}
}