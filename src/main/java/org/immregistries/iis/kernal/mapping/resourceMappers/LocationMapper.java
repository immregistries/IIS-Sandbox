package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.OrgLocation;

public interface LocationMapper<Location extends IBaseResource> extends IisResourceMasterMapper<OrgLocation, Location> {
	default String fhirType() {
		return LOCATION;
	}

	public static final String LOCATION = "Location";

	default Class<OrgLocation> localType() {
		return OrgLocation.class;
	}

	public String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
	// public Location fhirResource(OrgLocation ol);
	// public OrgLocation localObject(Location l);
}
