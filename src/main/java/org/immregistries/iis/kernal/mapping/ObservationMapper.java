package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static Observation getFhirObservation(ObservationMaster observationMaster, ObservationReported observationReported)  {
		Observation o = new Observation();
		if (observationMaster != null) {
			o.setId(observationMaster.getObservationId());
			o.addIdentifier(MappingHelper.getFhirIdentifier( "ObservationMaster",observationMaster.getObservationId()));
			o.addPartOf(MappingHelper.getFhirReference("Immunization","VaccinationMaster", observationMaster.getVaccination().getVaccinationId()));
			if (observationMaster.getObservationReported() != null && observationReported == null) {
				o.addPartOf(MappingHelper.getFhirReference("Observation","ObservationReported",
					observationMaster.getObservationReported().getObservationReportedId()));
			}
			o.setSubject(MappingHelper.getFhirReference("Patient","PatientMaster",observationMaster.getPatient().getPatientId()));
			o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
			o.setValue(new CodeableConcept().setText(observationMaster.getValueCode()));
		}


		if (observationReported != null) {
			// Observation reported id takes over observation Master Id
			o.setId(observationReported.getObservationReportedId());
			o.addIdentifier(MappingHelper.getFhirIdentifier("ObservationReported",observationReported.getObservationReportedId()));
			if (o.getPartOf().size() == 0) {
				o.addPartOf(MappingHelper.getFhirReference("Immunization","VaccinationReported",observationReported.getVaccinationReported().getVaccinationReportedExternalLink()));
			}
			if(observationReported.getPatientReported() != null) {
				o.setSubject(MappingHelper.getFhirReference("Patient","PatientReported",observationReported.getPatientReported().getPatientReportedExternalLink()));
			}
			if (observationReported.getObservation() != null && observationMaster == null) {
				o.addPartOf(MappingHelper.getFhirReference("Observation","ObservationMaster", observationReported.getObservation().getObservationId()));
			}
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
