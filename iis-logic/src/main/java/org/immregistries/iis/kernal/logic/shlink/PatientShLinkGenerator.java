package org.immregistries.iis.kernal.logic.shlink;

import org.immregistries.iis.kernal.model.shlink.ShLinkPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


@Service
public class PatientShLinkGenerator {

	public @NotNull ShLinkPayload generatePatientShLinkPayload(String manifestUrl) {
		ShLinkPayload shLinkPayload = new ShLinkPayload();
		shLinkPayload.setUrl(manifestUrl);
		shLinkPayload.setLabel("Generated for testing");
		shLinkPayload.setKey(null);
		shLinkPayload.setFlag("LP");
		shLinkPayload.setExp(10000000L);
		return shLinkPayload;
	}

}
