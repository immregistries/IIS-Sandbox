package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.OrgLocation;

public interface LocationMapper<Location> {
	public String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
	public Location getFhirResource(OrgLocation ol);
	public OrgLocation orgLocationFromFhir(Location l);
}
