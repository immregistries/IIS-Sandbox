package org.immregistries.iis.kernal.logic.match;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@Conditional(OnR5Condition.class)
public class VaccinationDedupConversionServiceR5 implements VaccinationDedupConversionService<org.hl7.fhir.r5.model.Immunization> {

	public org.immregistries.vaccination_deduplication.Immunization convert(org.hl7.fhir.r5.model.Immunization fhirImmunization, RequestPartitionId theRequestPartitionId) {
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		i1.setCVX(fhirImmunization.getVaccineCode().getCode(ImmunizationMapper.CVX_SYSTEM));
		if (fhirImmunization.hasManufacturer()) {
			i1.setMVX(fhirImmunization.getManufacturer().getReference().getIdentifier().getValue());
		}
		try {
			if (fhirImmunization.hasOccurrenceStringType()) {
				i1.setDate(fhirImmunization.getOccurrenceStringType().getValue()); // TODO parse correctly
			} else if (fhirImmunization.hasOccurrenceDateTimeType()) {
				i1.setDate(fhirImmunization.getOccurrenceDateTimeType().getValue());
			}
		} catch (ParseException ignored) {
//			e.printStackTrace();
		}

		i1.setLotNumber(fhirImmunization.getLotNumber());

		if (fhirImmunization.getPrimarySource()) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else if (fhirImmunization.hasInformationSource()
			&& fhirImmunization.getInformationSource().getConcept() != null
			&& StringUtils.isNotBlank(fhirImmunization.getInformationSource().getConcept().getCode(ImmunizationMapper.INFORMATION_SOURCE))
			&& fhirImmunization.getInformationSource().getConcept().getCode(ImmunizationMapper.INFORMATION_SOURCE).equals("00")) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}

		if (fhirImmunization.hasInformationSource()) { // TODO improve organisation naming and designation among tenancy or in resource info
			if (fhirImmunization.getInformationSource().getReference() != null) {
				if (fhirImmunization.getInformationSource().getReference().getIdentifier() != null) {
					i1.setOrganisationID(fhirImmunization.getInformationSource().getReference().getIdentifier().getValue());
				} else if (fhirImmunization.getInformationSource().getReference().getReference() != null
					&& fhirImmunization.getInformationSource().getReference().getReference().startsWith("Organization/")) {
					i1.setOrganisationID(fhirImmunization.getInformationSource().getReference().getReference()); // TODO get organisation name from db
				}
			}
		}
		if ((i1.getOrganisationID() == null || i1.getOrganisationID().isBlank()) && theRequestPartitionId.hasPartitionNames()) {
			i1.setOrganisationID(theRequestPartitionId.getFirstPartitionNameOrNull());
		}
		return i1;
	}
}
