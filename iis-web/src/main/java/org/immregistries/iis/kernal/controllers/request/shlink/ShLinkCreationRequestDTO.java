package org.immregistries.iis.kernal.controllers.request.shlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.immregistries.iis.kernal.controllers.IisRestParam;

/**
 * DTO for SMART Health Link creation using project-specific constants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShLinkCreationRequestDTO {

	@JsonProperty(IisRestParam.KEY_ID)
	private String keyId;

	@JsonProperty(IisRestParam.ShLink.SECRET_KEY)
	private String secretKey;

	@JsonProperty(IisRestParam.PATIENT_ID)
	private String patientId;

	@JsonProperty(IisRestParam.ShLink.FLAG)
	private String flag;

	@JsonProperty(IisRestParam.ShLink.PASSCODE)
	private String passcode;

	@JsonProperty(IisRestParam.ShLink.EXP)
	@Builder.Default
	@JsonSetter(nulls = Nulls.SKIP)
	private String exp = "10000000";

	@JsonProperty("label")
	private String label;

	@JsonProperty("description")
	private String description;

}