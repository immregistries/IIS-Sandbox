package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.OrgLocation;

public abstract class LocationMapper<Location extends IAnyResource>
		implements IisResourceMasterMapper<OrgLocation, Location> {
	public String fhirType() {
		return LOCATION;
	}

	public static final String LOCATION = "Location";

	public Class<OrgLocation> localType() {
		return OrgLocation.class;
	}

	public static final String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
	// public Location fhirResource(OrgLocation ol);
	// public OrgLocation localObject(Location l);
}
