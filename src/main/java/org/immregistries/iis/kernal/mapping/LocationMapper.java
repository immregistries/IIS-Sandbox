package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.Address;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Location;
import org.immregistries.iis.kernal.model.OrgLocation;

public class LocationMapper {

	public static Location fhirLocation(OrgLocation ol) {
		Location location = new Location();
		if (ol != null) {
			location.setId(ol.getOrgLocationId());
			location.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.ORG_LOCATION, ol.getOrgFacilityCode()));
			location.setName(ol.getOrgFacilityName());

			Address address = location.getAddress();
			if (!ol.getAddressLine1().isBlank()) {
				address.addLine(ol.getAddressLine1());
			}
			if (!ol.getAddressLine2().isBlank()) {
				address.addLine(ol.getAddressLine2());
			}
			address.setCity(ol.getAddressCity());
			address.setState(ol.getAddressState());
			address.setPostalCode(ol.getAddressZip());
			address.setCountry(ol.getAddressCountry());
		}
		return  location;
	}

	public static OrgLocation orgLocationFromFhir(Location l) {
		OrgLocation orgLocation = new OrgLocation();
		orgLocation.setOrgLocationId(new IdType(l.getId()).getIdPart());
		orgLocation.setOrgFacilityCode(l.getIdentifierFirstRep().getValue());
		orgLocation.setOrgFacilityName(l.getName());
		if (l.getTypeFirstRep().getText() != null) {
			orgLocation.setLocationType(l.getTypeFirstRep().getText());
		}
		if (l.getAddress() != null) {
			if (l.getAddress().getLine().size() >= 1) {
				orgLocation.setAddressLine1(l.getAddress().getLine().get(0).getValueNotNull());
			}
			if (l.getAddress().getLine().size() > 1) {
				orgLocation.setAddressLine2(l.getAddress().getLine().get(1).getValueNotNull());
			}
			if (l.getAddress().getCity() != null){
				orgLocation.setAddressCity(l.getAddress().getCity());
			}
			if (l.getAddress().getState() != null){
				orgLocation.setAddressState(l.getAddress().getState());
			}
			if (l.getAddress().getPostalCode() != null){
				orgLocation.setAddressZip(l.getAddress().getPostalCode());
			}
			if (l.getAddress().getCountry() != null){
				orgLocation.setAddressCountry(l.getAddress().getCountry());
			}
		}
		return orgLocation;
	}

}
