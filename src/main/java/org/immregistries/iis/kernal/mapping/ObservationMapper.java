package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static Observation getFhirObservation(ObservationMaster observationMaster, ObservationReported or)  {
		Observation o = new Observation();
		if (observationMaster != null) {
			o.addIdentifier(MappingHelper.getFhirIdentifier( "ObservationMaster",observationMaster.getObservationId()));
			o.addPartOf(MappingHelper.getFhirReference("Immunization","VaccinationMaster", observationMaster.getVaccination().getVaccinationId()));
//			if (observationMaster.getObservationReported() != null && or == null) {
//				o.addPartOf(MappingHelper.getFhirReference("Observation","ObservationReported",
//					observationMaster.getObservationReported().getObservationReportedId()));
//			}
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				"identifierCode",observationMaster.getIdentifierCode()));
			o.setSubject(MappingHelper.getFhirReference("Patient","PatientMaster",observationMaster.getPatient().getPatientId()));
			o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
			o.setValue(new CodeableConcept(new Coding().setCode(observationMaster.getValueCode())));
		}

		if (or != null) {
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
			o.setValue(new CodeableConcept(new Coding()
				.setCode(or.getValueCode())
				.setSystem(or.getValueTable())
				.setDisplay(or.getValueLabel())));
			o.setMethod(new CodeableConcept()).getMethod().addCoding()
				.setCode(or.getMethodCode())
				.setSystem(or.getMethodTable())
				.setDisplay(or.getMethodLabel());
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				or.getIdentifierTable(),or.getIdentifierCode())); //TODO label
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
		ObservationReported or = new ObservationReported();
		or.setObservationReportedId(o.getCode().getCode("ObservationReported"));
//		or.setVaccinationReported();
//		or.setObservation();
//		or.setPatientReported();
		or.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		or.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		or.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		or.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		or.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		or.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());
		for (Identifier identifier: o.getIdentifier()) {
			switch (identifier.getSystem()) {
				case "Observation": {
					break;
				}
				case "ObservationMaster": {
					break;
				}
				case "ObservationReported": {
					break;
				} default: {
					or.setIdentifierCode(identifier.getValue());
					or.setIdentifierTable(identifier.getSystem());
				}
			}
		}
		for (Observation.ObservationComponentComponent component: o.getComponent()) {
			if (component.getCode().getText().equals("observationDate")) {
				or.setObservationDate(component.getValueDateTimeType().getValue());
			}
		}
		or.setUnitsCode(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getCodingFirstRep().getCode());
		or.setUnitsTable(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getText());
		or.setUnitsLabel(o.getReferenceRangeFirstRep().getText());
		or.setResultStatus(o.getInterpretationFirstRep().getCodingFirstRep().getCode());

		return  or;
	}

	public static ObservationMaster getObservationMaster(Observation o){
		ObservationMaster om = new ObservationMaster();
		om.setObservationId(o.getCode().getCode("ObservationMaster"));
		om.setIdentifierCode(o.getCode().getCode("identifierCode"));
//		om.setPatient();
//		om.setVaccination();
//		om.setObservationReported();
		om.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		return  om;
	}


}
