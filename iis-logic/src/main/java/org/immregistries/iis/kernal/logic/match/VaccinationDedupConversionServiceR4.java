package org.immregistries.iis.kernal.logic.match;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import org.hl7.fhir.r4.model.Coding;
import org.immregistries.iis.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@Conditional(OnR4Condition.class)
public class VaccinationDedupConversionServiceR4 implements VaccinationDedupConversionService<org.hl7.fhir.r4.model.Immunization> {

	public org.immregistries.vaccination_deduplication.Immunization convert(org.hl7.fhir.r4.model.Immunization fhirImmunization, RequestPartitionId theRequestPartitionId) {
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		Coding cvx = MappingHelper.filterCodeableConceptR4(fhirImmunization.getVaccineCode(), ImmunizationMapper.CVX_SYSTEM);
		if (cvx != null) {
			i1.setCVX(cvx.getCode());
		}
		if (fhirImmunization.hasManufacturer()) {
			i1.setMVX(fhirImmunization.getManufacturer().getIdentifier().getValue());
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
//		} else if (fhirImmunization.hasInformationSource()
//			&& fhirImmunization.getInformationSource().getConcept() != null
//			&& StringUtils.isNotBlank(fhirImmunization.getInformationSource().getConcept().getCode(ImmunizationMapperR4.INFORMATION_SOURCE))
//			&& fhirImmunization.getInformationSource().getConcept().getCode(ImmunizationMapperR4.INFORMATION_SOURCE).equals("00")) {
//			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}

//		if (fhirImmunization.hasInformationSource()) { // TODO improve organisation naming and designation among tenancy or in resource info
//			if (fhirImmunization.getInformationSource().getReference() != null) {
//				if (fhirImmunization.getInformationSource().getReference().getIdentifier() != null) {
//					i1.setOrganisationID(fhirImmunization.getInformationSource().getReference().getIdentifier().getValue());
//				} else if (fhirImmunization.getInformationSource().getReference().getReference() != null
//					&& fhirImmunization.getInformationSource().getReference().getReference().startsWith("Organisation/")) {
//					i1.setOrganisationID(fhirImmunization.getInformationSource().getReference().getReference()); // TODO get organisation name from db
//				}
//			}
//		}
		if ((i1.getOrganisationID() == null || i1.getOrganisationID().isBlank()) && theRequestPartitionId.hasPartitionNames()) {
			i1.setOrganisationID(theRequestPartitionId.getFirstPartitionNameOrNull());
		}
		return i1;
	}
}
