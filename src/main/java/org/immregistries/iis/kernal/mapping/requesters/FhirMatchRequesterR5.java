package org.immregistries.iis.kernal.mapping.requesters;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.DecimalType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.PatientMapperR5;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@Conditional(OnR5Condition.class)
public class FhirMatchRequesterR5 implements FhirMatchRequester {
	@Autowired
	IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	PatientMapperR5 patientMapper;


	public PatientMaster matchPatient(List<PatientReported> multipleMatches, IisPatient patientForMatchQuery,
												 Date cutoff) {
		PatientMaster singleMatch = null;
		Bundle matches = iisFhirClientFactory.getOrCreateFhirClientFromContext()
			.operation().onType(Patient.class)
			.named("match")
			.withParameter(Parameters.class, "resource", patientMapper.fhirObject(patientForMatchQuery))
			.returnResourceType(Bundle.class).execute();
		BigDecimal singleMatchScore = new BigDecimal(-1);
		for (Bundle.BundleEntryComponent entry : matches.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				PatientMaster patientMaster = patientMapper.localObjectMaster(patient);
				/*
				 * Filter for flavours previously configured SNAIL
				 */
				if (cutoff != null && cutoff.before(patientMaster.getReportedDate())) {
					break;
				}

				/*
				 * TODO ask Nathan to assert workflow
				 */
				if (FhirRequesterUtil.isGoldenRecord(patient)) {
					if (singleMatch == null) {
						if (!entry.getSearch().hasScore()) {
							singleMatch = patientMaster;
						} else if (entry.getSearch().getScoreElement().compareTo(new DecimalType(
							Math.max(MINIMAL_MATCHING_SCORE, singleMatchScore.toBigInteger().intValue()))) >= 0) {
							singleMatch = patientMaster;
							singleMatchScore = entry.getSearch().getScore();
						}
					}
				}
				multipleMatches.add(patientMapper.localObjectReported(patient));
			}
		}
		return singleMatch;
	}
}
