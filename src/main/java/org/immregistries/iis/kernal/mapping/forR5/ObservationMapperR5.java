package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.MappingHelper.IMMUNIZATION;
import static org.immregistries.iis.kernal.mapping.MappingHelper.PATIENT;

@Service("ObservationMapperR5")
@Conditional(OnR5Condition.class)
public class ObservationMapperR5 implements ObservationMapper<Observation> {
	@Autowired
	FhirRequester fhirRequests;

	public Observation getFhirResource(ObservationReported observationReported)  {
		Observation o = new Observation();
		o.setId(observationReported.getObservationId());
		if (!observationReported.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new Reference().setReference(IMMUNIZATION + "/" + observationReported.getVaccinationReportedId()));
		}
		if(observationReported.getPatientReportedId() != null) {
			o.setSubject(new Reference().setReference(PATIENT + "/"+observationReported.getPatientReportedId()));
//				o.setSubject(MappingHelper.getFhirReference(PATIENT,PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
		}
		o.setValue(new CodeableConcept(new Coding()
			.setCode(observationReported.getValueCode())
			.setSystem(observationReported.getValueTable())
			.setDisplay(observationReported.getValueLabel())));
		o.setMethod(new CodeableConcept()).getMethod().addCoding()
			.setCode(observationReported.getMethodCode())
			.setSystem(observationReported.getMethodTable())
			.setDisplay(observationReported.getMethodLabel());
		o.addIdentifier(MappingHelper.getFhirIdentifierR5(
			observationReported.getIdentifierTable(),observationReported.getIdentifierCode())); //TODO label
		o.addComponent().setValue(new DateTimeType(observationReported.getObservationDate()))
			.setCode(new CodeableConcept().setText(OBSERVATION_DATE));
		o.addReferenceRange().setText(observationReported.getUnitsLabel())
			.addAppliesTo().setText(observationReported.getUnitsTable())
			.addCoding().setCode(observationReported.getUnitsCode());
		o.addInterpretation().setText(RESULT_STATUS)
			.addCoding().setCode(observationReported.getResultStatus());
		return o;

	}

	public ObservationReported getReportedWithMaster(Observation observation, IGenericClient fhirClient){
		ObservationReported observationReported = getReported(observation);
		observationReported.setObservationMaster(
			fhirRequests.searchObservationMaster(
				new SearchParameterMap(Observation.SP_IDENTIFIER,new TokenParam().setSystem(observationReported.getIdentifierTable()).setValue(observationReported.getIdentifierCode())))
		);
//                    Observation.IDENTIFIER.exactly().systemAndIdentifier(observationReported.getIdentifierTable(),observationReported.getIdentifierCode())
//			));
		return observationReported;
	}


	public ObservationReported getReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		observationReported.setObservationId(o.getId());
//		observationReported.setVaccinationReportedId(o.getPartOfFirstRep().getId());
		observationReported.setVaccinationReportedId(o.getPartOf().stream().filter(ref -> ref.getReference().startsWith("Immunization/")).findFirst().orElse(new Reference("")).getId());
		observationReported.setPatientReportedId(o.getSubject().getId());
		observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());

//		Identifier identifier = o.getIdentifierFirstRep();
//		observationReported.setIdentifierCode(identifier.getValue());
//		observationReported.setIdentifierTable(identifier.getSystem());
		Coding identifierCoding = o.getCode().getCoding().stream().filter(coding -> coding.getSystem().equals(IDENTIFIER_CODE)).findFirst().orElse(new Coding());
		observationReported.setIdentifierCode(identifierCoding.getCode());
		observationReported.setIdentifierTable(identifierCoding.getSystem());
		observationReported.setIdentifierLabel(identifierCoding.getDisplay());


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
		return getReported(o);
	}


}
