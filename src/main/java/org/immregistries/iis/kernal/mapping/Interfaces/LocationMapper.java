package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.OrgLocation;

public interface LocationMapper<Location> {
	public Location getFhirResource(OrgLocation ol);
	public OrgLocation orgLocationFromFhir(Location l);
}
