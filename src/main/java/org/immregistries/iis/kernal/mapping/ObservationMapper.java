package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public class ObservationMapper {
	public static final String IDENTIFIER_CODE = "identifierCode";
	public static final String OBSERVATION_DATE = "observationDate";
	public static final String RESULT_STATUS = "resultStatus";

	public static Observation getFhirResource(ObservationMaster observationMaster, ObservationReported observationReported)  {
		Observation o = new Observation();
		if (observationMaster != null) {
			o.addIdentifier(MappingHelper.getFhirIdentifier( MappingHelper.OBSERVATION_MASTER,observationMaster.getObservationId()));
			o.addPartOf(MappingHelper.getFhirReference(MappingHelper.IMMUNIZATION,MappingHelper.VACCINATION_MASTER, observationMaster.getVaccination().getVaccinationId()));
			o.addPartOf(new Reference(MappingHelper.IMMUNIZATION + "/" + observationMaster.getVaccination().getVaccinationId()));
//			if (observationMaster.getObservationReported() != null && observationReported == null) {
//				o.addPartOf(MappingHelper.getFhirReference(MappingHelper.OBSERVATION,MappingHelper.OBSERVATION_REPORTED,
//					observationMaster.getObservationReported().getObservationReportedId()));
//			}
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				IDENTIFIER_CODE,observationMaster.getIdentifierCode()));
			o.setCode(new CodeableConcept().setText(observationMaster.getIdentifierCode()));
			o.setValue(new CodeableConcept(new Coding().setCode(observationMaster.getValueCode())));
		}

		if (observationReported != null) {
			o.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.OBSERVATION_REPORTED,observationReported.getObservationReportedId()));
			if (o.getPartOf().size() == 0) {
				o.addPartOf(new Reference(MappingHelper.IMMUNIZATION + "/" + observationReported.getVaccinationReported().getVaccinationReportedExternalLink()));
			}
			if(observationReported.getPatientReported() != null) {
				o.setSubject(new Reference().setReference("Patient/"+observationReported.getPatientReported().getPatientReportedId())); // TODO reactivate

//				o.setSubject(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
			}
			if (observationReported.getObservation() != null && observationMaster == null) {
				o.addPartOf(MappingHelper.getFhirReference(MappingHelper.OBSERVATION,MappingHelper.OBSERVATION_MASTER, observationReported.getObservation().getObservationId()));
			}
			o.setValue(new CodeableConcept(new Coding()
				.setCode(observationReported.getValueCode())
				.setSystem(observationReported.getValueTable())
				.setDisplay(observationReported.getValueLabel())));
			o.setMethod(new CodeableConcept()).getMethod().addCoding()
				.setCode(observationReported.getMethodCode())
				.setSystem(observationReported.getMethodTable())
				.setDisplay(observationReported.getMethodLabel());
			o.addIdentifier(MappingHelper.getFhirIdentifier(
				observationReported.getIdentifierTable(),observationReported.getIdentifierCode())); //TODO label
			o.addComponent().setValue(new DateTimeType(observationReported.getObservationDate()))
				.setCode(new CodeableConcept().setText(OBSERVATION_DATE));
			o.addReferenceRange().setText(observationReported.getUnitsLabel())
				.addAppliesTo().setText(observationReported.getUnitsTable())
				.addCoding().setCode(observationReported.getUnitsCode());
			o.addInterpretation().setText(RESULT_STATUS)
				.addCoding().setCode(observationReported.getResultStatus());
		}
		return o;

	}


	public static ObservationReported getReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		observationReported.setObservationReportedId(o.getCode().getCode(MappingHelper.OBSERVATION_REPORTED));
//		observationReported.setVaccinationReported();
//		observationReported.setObservation();
//		observationReported.setPatientReported();
		observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());
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
					observationReported.setIdentifierCode(identifier.getValue());
					observationReported.setIdentifierTable(identifier.getSystem());
				}
			}
		}
		for (Observation.ObservationComponentComponent component: o.getComponent()) {
			if (component.getCode().getText().equals(OBSERVATION_DATE)) {
				observationReported.setObservationDate(component.getValueDateTimeType().getValue());
			}
		}
		observationReported.setUnitsCode(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getCodingFirstRep().getCode());
		observationReported.setUnitsTable(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getText());
		observationReported.setUnitsLabel(o.getReferenceRangeFirstRep().getText());
		observationReported.setResultStatus(o.getInterpretationFirstRep().getCodingFirstRep().getCode());

		return  observationReported;
	}

	public static ObservationMaster getMaster(Observation o){
		ObservationMaster observationMaster = new ObservationMaster();
		observationMaster.setObservationId(o.getCode().getCode(MappingHelper.OBSERVATION_MASTER));
		observationMaster.setIdentifierCode(o.getCode().getCode(IDENTIFIER_CODE));
//		observationMaster.setPatient();
//		observationMaster.setVaccination();
//		observationMaster.setObservationReported();
		observationMaster.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		return  observationMaster;
	}


}
