package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static Observation getFhirObservation(ObservationMaster observationMaster, ObservationReported or)  {
		Observation o = new Observation();
		if (observationMaster != null) {
//			o.setId(observationMaster.getObservationId());
			o.addIdentifier(MappingHelper.getFhirIdentifier( "ObservationMaster",observationMaster.getObservationId()));
			o.addPartOf(MappingHelper.getFhirReference("Immunization","VaccinationMaster", observationMaster.getVaccination().getVaccinationId()));
			if (observationMaster.getObservationReported() != null && or == null) {
				o.addPartOf(MappingHelper.getFhirReference("Observation","ObservationReported",
					observationMaster.getObservationReported().getObservationReportedId()));
			}
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				"identifierCode",observationMaster.getIdentifierCode()));
			o.setSubject(MappingHelper.getFhirReference("Patient","PatientMaster",observationMaster.getPatient().getPatientId()));
			o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
			o.setValue(new Coding().setCode(observationMaster.getValueCode()));
		}

		if (or != null) {
//			o.setId(or.getObservationReportedId());
			o.addIdentifier(MappingHelper.getFhirIdentifier("ObservationReported",or.getObservationReportedId()));
			if (o.getPartOf().size() == 0) {
				o.addPartOf(MappingHelper.getFhirReference("Immunization","VaccinationReported",or.getVaccinationReported().getVaccinationReportedExternalLink()));
			}
			if(or.getPatientReported() != null) {
				o.setSubject(MappingHelper.getFhirReference("Patient","PatientReported",or.getPatientReported().getPatientReportedExternalLink()));
			}
			if (or.getObservation() != null && observationMaster == null) {
				o.addPartOf(MappingHelper.getFhirReference("Observation","ObservationMaster", or.getObservation().getObservationId()));
			}
			o.setValue(new Coding()
				.setCode(or.getValueCode())
				.setSystem(or.getValueTable())
				.setDisplay(or.getValueLabel()));
			o.setMethod(new CodeableConcept()).getMethod().addCoding()
				.setCode(or.getMethodCode())
				.setSystem(or.getMethodTable())
				.setDisplay(or.getMethodLabel());
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				or.getIdentifierTable(),or.getIdentifierCode()));
			o.addComponent().setValue(new DateTimeType(or.getObservationDate()))
				.setCode(new CodeableConcept().setText("observationDate"));
			o.addReferenceRange().setText(or.getUnitsLabel())
				.addAppliesTo().setText(or.getUnitsTable())
				.addCoding().setCode(or.getUnitsCode());
			o.addInterpretation().setText("resultStatus")
				.addCoding().setCode(or.getResultStatus());
		}
		return o;

	}


	public static ObservationReported getObservationReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setObservationReportedId(o.getId());

		return  observationReported;
	}

	public static ObservationMaster getObservationMaster(Observation o){
		ObservationMaster observationMaster = new ObservationMaster();
		observationMaster.setIdentifierCode(o.getCode().getText());


		return  observationMaster;
	}


}
