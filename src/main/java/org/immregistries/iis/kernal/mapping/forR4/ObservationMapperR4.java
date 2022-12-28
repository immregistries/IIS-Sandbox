package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.repository.FhirRequester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.MappingHelper.*;

@Service
@Conditional(OnR4Condition.class)
public class ObservationMapperR4 implements ObservationMapper<Observation> {
	@Autowired
	FhirRequester fhirRequests;

	public Observation getFhirResource(ObservationReported observationReported)  {
		Observation o = new Observation();
		o.setId(observationReported.getObservationReportedId());
		if (!observationReported.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new org.hl7.fhir.r4.model.Reference().setReference(IMMUNIZATION + "/" + observationReported.getVaccinationReportedId()));
		}
		if(observationReported.getPatientReportedId() != null) {
			o.setSubject(new org.hl7.fhir.r4.model.Reference().setReference(PATIENT + "/"+observationReported.getPatientReportedId()));
//				o.setSubject(MappingHelper.getFhirReference(PATIENT,PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
		}
		o.setValue(new org.hl7.fhir.r4.model.CodeableConcept(new org.hl7.fhir.r4.model.Coding()
			.setCode(observationReported.getValueCode())
			.setSystem(observationReported.getValueTable())
			.setDisplay(observationReported.getValueLabel())));
		o.setMethod(new org.hl7.fhir.r4.model.CodeableConcept()).getMethod().addCoding()
			.setCode(observationReported.getMethodCode())
			.setSystem(observationReported.getMethodTable())
			.setDisplay(observationReported.getMethodLabel());
		o.addIdentifier(MappingHelper.getFhirR4Identifier(
			observationReported.getIdentifierTable(),observationReported.getIdentifierCode())); //TODO label
		o.addComponent().setValue(new org.hl7.fhir.r4.model.DateTimeType(observationReported.getObservationDate()))
			.setCode(new org.hl7.fhir.r4.model.CodeableConcept().setText(OBSERVATION_DATE));
		o.addReferenceRange().setText(observationReported.getUnitsLabel())
			.addAppliesTo().setText(observationReported.getUnitsTable())
			.addCoding().setCode(observationReported.getUnitsCode());
		o.addInterpretation().setText(RESULT_STATUS)
			.addCoding().setCode(observationReported.getResultStatus());
		return o;

	}

	public ObservationReported getReportedWithMaster(Observation observation, FhirRequester fhirRequests, IGenericClient fhirClient){
		ObservationReported observationReported = getReported(observation);
		observationReported.setObservation(
			fhirRequests.searchObservationMaster(
				Observation.IDENTIFIER.exactly().systemAndIdentifier(observationReported.getIdentifierTable(),observationReported.getIdentifierCode())
			));
		return observationReported;
	}


	public ObservationReported getReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		observationReported.setObservationReportedId(o.getId());
		observationReported.setVaccinationReportedId(o.getPartOfFirstRep().getId());
		observationReported.setPatientReportedId(o.getSubject().getId());
		observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());

		org.hl7.fhir.r4.model.Identifier identifier = o.getIdentifierFirstRep();
		observationReported.setIdentifierCode(identifier.getValue());
		observationReported.setIdentifierTable(identifier.getSystem());

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

	public ObservationMaster getMaster(Observation o){
		ObservationMaster observationMaster = new ObservationMaster();
		observationMaster.setObservationId(o.getId());
		observationMaster.setIdentifierCode(o.getCode().getCoding().stream().filter(coding -> coding.getSystem().equals(IDENTIFIER_CODE)).findFirst().orElse(new Coding()).getCode());
		observationMaster.setPatientId(o.getSubject().getId());
		observationMaster.setVaccinationId(o.getPartOf().stream().filter(ref -> ref.getReference().startsWith("Immunization/")).findFirst().orElse(new Reference("")).getId());
//		observationMaster.setObservationReported();
		observationMaster.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		return  observationMaster;
	}



}