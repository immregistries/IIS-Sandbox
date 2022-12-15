package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.immregistries.iis.kernal.mapping.Interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class LocationMapperR4 implements LocationMapper<Location> {

	public Location getFhirResource(OrgLocation ol) {

		if (ol != null) {
			Location location = new Location();
			location.setId(ol.getOrgLocationId());
			location.addIdentifier(new Identifier().setSystem(MappingHelper.ORG_LOCATION).setValue(ol.getOrgFacilityCode()));
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
			return  location;
		}
		return null;
	}

	public OrgLocation orgLocationFromFhir(Location l) {
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
