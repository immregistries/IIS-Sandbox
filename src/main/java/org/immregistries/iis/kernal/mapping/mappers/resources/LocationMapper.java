package org.immregistries.iis.kernal.mapping.mappers.resources;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.OrgLocation;

public abstract class LocationMapper<Location extends IAnyResource>
		implements IisResourceMapper<OrgLocation, Location> {
	public String fhirTypeName() {
		return LOCATION_FHIR_TYPE_NAME;
	}

	public static final String LOCATION_FHIR_TYPE_NAME = "Location";

	public Class<OrgLocation> localType() {
		return OrgLocation.class;
	}

	public static final String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
	// public Location fhirResource(OrgLocation ol);
	// public OrgLocation localObject(Location l);
}
