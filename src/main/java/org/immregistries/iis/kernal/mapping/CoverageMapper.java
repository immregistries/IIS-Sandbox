package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.Coverage;
import org.hl7.fhir.r5.model.Period;
import org.immregistries.iis.kernal.model.PatientReported;

public class CoverageMapper {
	public Coverage getPatientCoverage(PatientReported patientReported) {
		Coverage coverage = new Coverage();
		coverage.setBeneficiary(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_REPORTED,patientReported.getPatientReportedId()));
		coverage.setPeriod(new Period().setEnd(patientReported.getProtectionIndicatorDate()));
		return coverage;
	}

	public void fillPatientReportedWithFhir(PatientReported patientReported, Coverage coverage) {
//		patientReported.setProtectionIndicatorDate(coverage.getPeriod().getEnd());
//		patientReported.setProtectionIndicator(coverage.get);
//		coverage.setPeriod(new Period().setEnd(patientReported.getProtectionIndicatorDate()));
//		return coverage;
	}


}
