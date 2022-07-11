package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static final String IDENTIFIER_CODE = "identifierCode";
	public static final String OBSERVATION_DATE = "observationDate";
	public static final String RESULT_STATUS = "resultStatus";

	public static Observation getFhirObservation(ObservationMaster observationMaster, ObservationReported or)  {
		Observation o = new Observation();
		if (observationMaster != null) {
			o.addIdentifier(MappingHelper.getFhirIdentifier( MappingHelper.OBSERVATION_MASTER,observationMaster.getObservationId()));
			o.addPartOf(MappingHelper.getFhirReference(MappingHelper.IMMUNIZATION,MappingHelper.VACCINATION_MASTER, observationMaster.getVaccination().getVaccinationId()));
//			if (observationMaster.getObservationReported() != null && or == null) {
//				o.addPartOf(MappingHelper.getFhirReference(MappingHelper.OBSERVATION,MappingHelper.OBSERVATION_REPORTED,
//					observationMaster.getObservationReported().getObservationReportedId()));
//			}
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				IDENTIFIER_CODE,observationMaster.getIdentifierCode()));
			o.setSubject(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_MASTER,observationMaster.getPatient().getPatientId()));
			o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
			o.setValue(new CodeableConcept(new Coding().setCode(observationMaster.getValueCode())));
		}

		if (or != null) {
			o.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.OBSERVATION_REPORTED,or.getObservationReportedId()));
			if (o.getPartOf().size() == 0) {
				o.addPartOf(MappingHelper.getFhirReference(MappingHelper.IMMUNIZATION,MappingHelper.VACCINATION_REPORTED,or.getVaccinationReported().getVaccinationReportedExternalLink()));
			}
			if(or.getPatientReported() != null) {
				o.setSubject(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_REPORTED,or.getPatientReported().getPatientReportedExternalLink()));
			}
			if (or.getObservation() != null && observationMaster == null) {
				o.addPartOf(MappingHelper.getFhirReference(MappingHelper.OBSERVATION,MappingHelper.OBSERVATION_MASTER, or.getObservation().getObservationId()));
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
				.setCode(new CodeableConcept().setText(OBSERVATION_DATE));
			o.addReferenceRange().setText(or.getUnitsLabel())
				.addAppliesTo().setText(or.getUnitsTable())
				.addCoding().setCode(or.getUnitsCode());
			o.addInterpretation().setText(RESULT_STATUS)
				.addCoding().setCode(or.getResultStatus());
		}
		return o;

	}


	public static ObservationReported getObservationReported(Observation o){
		ObservationReported or = new ObservationReported();
		or.setObservationReportedId(o.getCode().getCode(MappingHelper.OBSERVATION_REPORTED));
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
				case MappingHelper.OBSERVATION: {
					break;
				}
				case MappingHelper.OBSERVATION_MASTER: {
					break;
				}
				case MappingHelper.OBSERVATION_REPORTED: {
					break;
				} default: {
					or.setIdentifierCode(identifier.getValue());
					or.setIdentifierTable(identifier.getSystem());
				}
			}
		}
		for (Observation.ObservationComponentComponent component: o.getComponent()) {
			if (component.getCode().getText().equals(OBSERVATION_DATE)) {
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
		om.setObservationId(o.getCode().getCode(MappingHelper.OBSERVATION_MASTER));
		om.setIdentifierCode(o.getCode().getCode(IDENTIFIER_CODE));
//		om.setPatient();
//		om.setVaccination();
//		om.setObservationReported();
		om.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		return  om;
	}


}
