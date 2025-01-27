package org.immregistries.iis.kernal.mapping.forR4;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.StringType;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class LocationMapperR4 implements LocationMapper<Location> {

	public org.hl7.fhir.r4.model.Location fhirResource(OrgLocation ol) {

		if (ol != null) {
			org.hl7.fhir.r4.model.Location location = new org.hl7.fhir.r4.model.Location();
			location.setId(ol.getOrgLocationId());
			location.addIdentifier(MappingHelper.getFhirIdentifierR4("", ol.getOrgFacilityCode()));
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

	public OrgLocation localObject(org.hl7.fhir.r4.model.Location l) {
		OrgLocation orgLocation = new OrgLocation();
		orgLocation.setOrgLocationId(l.getId());
		orgLocation.setOrgFacilityCode(l.getIdentifierFirstRep().getValue());
		orgLocation.setOrgFacilityName(l.getName());
		if (l.getTypeFirstRep().getCodingFirstRep().getCode() != null) {
			orgLocation.setLocationType(l.getTypeFirstRep().getCodingFirstRep().getCode());
		}

		Extension vfc = l.getExtensionByUrl(VFC_PROVIDER_PIN);
		if (vfc != null) {
			orgLocation.setVfcProviderPin(String.valueOf(vfc.getValue()));
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
