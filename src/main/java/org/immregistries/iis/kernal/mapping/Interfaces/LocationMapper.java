package org.immregistries.iis.kernal.mapping.Interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.OrgLocation;

public interface LocationMapper<Location extends IBaseResource> extends IisFhirMapperMaster<OrgLocation, Location> {
	public String VFC_PROVIDER_PIN = "VFC_PROVIDER_PIN";
//	public Location fhirResource(OrgLocation ol);
//	public OrgLocation localObject(Location l);
}
