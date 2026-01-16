package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.OrgLocation;

public interface LocationMapper<Location extends IAnyResource> extends IisResourceMasterMapper<OrgLocation, Location> {
	default String fhirType() {
		return LOCATION;
	}

	String LOCATION = "Location";

	default Class<OrgLocation> localType() {
		return OrgLocation.class;
	}

	String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
}
