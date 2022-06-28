package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.Code;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static Observation getFhirObservation(ObservationMaster observationMaster, ObservationReported observationReported)  {
		Observation o = new Observation();
		o.setId(Integer.toString(observationMaster.getObservationId()));
		o.addPartOf().setReference("Immunization/" + observationMaster.getVaccination().getVaccinationId());
		if (observationMaster.getObservationReported() != null) {
			o.addPartOf().setReference("Observation/" + observationMaster.getObservationReported().getObservationReportedId());
		}
		o.setSubject(new Reference("Patient/" + observationMaster.getPatient().getPatientId()));
		o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
		o.setValue(new CodeableConcept().setText(observationMaster.getValueCode()));
		return o;

	}

	public static Observation fillFhirObservation(Observation o, ObservationReported observationReported)  {
//		Observation o = new Observation();


		return o;

	}

	public static ObservationReported getObservationReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setObservationReportedId(o.getId());
//		observationReported.setpati

		return  observationReported;
	}

	public static ObservationMaster getObservationMaster(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setObservationReportedId(o.getId());
//		observationReported.setpati

		return  observationReported;
	}


}
