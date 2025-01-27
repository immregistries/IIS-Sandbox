package org.immregistries.iis.kernal.mapping.forR5;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.interfaces.LocationMapper;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class LocationMapperR5 implements LocationMapper<Location> {

	public Location fhirResource(OrgLocation ol) {

		if (ol != null) {
			Location location = new Location();
			location.setId(ol.getOrgLocationId());
			location.addIdentifier(new Identifier().setValue(ol.getOrgFacilityCode()));
			location.setName(ol.getOrgFacilityName());
			if (!ol.getVfcProviderPin().isBlank()) {
				location.addExtension().setUrl(VFC_PROVIDER_PIN).setValue(new StringType(ol.getVfcProviderPin()));
			}

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

	public OrgLocation localObject(Location l) {
		OrgLocation orgLocation = new OrgLocation();
		orgLocation.setOrgLocationId(l.getId());
		orgLocation.setOrgFacilityCode(l.getIdentifierFirstRep().getValue());
		orgLocation.setOrgFacilityName(l.getName());
		if (l.getTypeFirstRep().getCodingFirstRep().getCode() != null) {
			orgLocation.setLocationType(l.getTypeFirstRep().getCodingFirstRep().getCode());
		}

		Extension vfc = l.getExtensionByUrl(VFC_PROVIDER_PIN);
		if (vfc != null) {
			orgLocation.setVfcProviderPin(vfc.getValueStringType().getValue());
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
