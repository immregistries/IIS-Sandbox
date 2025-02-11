package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.immregistries.iis.kernal.mapping.MappingHelper.*;
import static org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper.RECORDED;

@Service
@Conditional(OnR4Condition.class)
public class ObservationMapperR4 implements ObservationMapper<Observation> {
	@Autowired
	private AbstractFhirRequester fhirRequests;

	public ObservationReported localObjectReportedWithMaster(Observation observation) {
		ObservationReported observationReported = localObjectReported(observation);
		observationReported.setObservationMaster(
			fhirRequests.searchObservationMaster(
				new SearchParameterMap(Observation.SP_IDENTIFIER, new TokenParam().setSystem(observationReported.getIdentifierTable()).setValue(observationReported.getIdentifierCode())) // TODO remove
//				Observation.IDENTIFIER.exactly().systemAndIdentifier(observationReported.getIdentifierTable(),observationReported.getIdentifierCode())
			));
		return observationReported;
	}

	public ObservationMaster localObject(Observation o) {
		return localObjectReported(o);
	}

	public Observation fhirResource(ObservationMaster om) {
		Observation o = new Observation();
		/*
		 * Id
		 */
		o.setId(StringUtils.defaultString(om.getObservationId()));
		/*
		 * Updated Date
		 */
		o.getMeta().setLastUpdated(om.getUpdatedDate());
		/*
		 * Recorded/Reported Date
		 */
		if (om.getReportedDate() != null) {
			o.addExtension()
				.setUrl(RECORDED)
				.setValue(new DateType(om.getReportedDate()));
		}
		/*
		 * Part of Vaccination
		 */
		if (StringUtils.isNotBlank(om.getVaccinationReportedId())) {
			o.addPartOf(new Reference().setReference(IMMUNIZATION + "/" + om.getVaccinationReportedId()));
		}
		/*
		 * Has member Observation TODO choose hasmember direction or change field
		 */
		if (StringUtils.isNotBlank(om.getPartOfObservationId())) {
			o.addHasMember(new Reference().setReference(OBSERVATION + "/" + om.getObservationId()));
		}
		/*
		 * Patient/Subject
		 */
		if (om.getPatientReportedId() != null) {
			o.setSubject(new Reference().setReference(PATIENT + "/" + om.getPatientReportedId()));
		}
		/*
		 * OBX-2 extension Value type
		 */
		if (om.getValueType() != null) {
			o.addExtension().setUrl(OBS_TYPE_OBX_2).setValue(new StringType(om.getValueType()));
		}
		/*
		 * OBX-3 observation code
		 */
		o.setCode(getValueCodeCodeableConcept(om));
		/*
		 * OBX-17 observation Method
		 */
		boolean methodNonNull = false;
		Coding observationMethodCoding = new Coding();
		if (om.getMethodCode() != null) {
			observationMethodCoding.setCode(om.getMethodCode());
			methodNonNull = true;
		}
		if (om.getMethodLabel() != null) {
			observationMethodCoding.setDisplay(om.getMethodLabel());
			methodNonNull = true;
		}
		if (om.getValueTable() != null) {
			observationMethodCoding.setSystem(om.getMethodTable());
			methodNonNull = true;
		}
		if (methodNonNull) {
			o.setMethod(new CodeableConcept().addCoding(observationMethodCoding));
		}
		/*
		 * OBX-14 Observation date
		 */
		if (om.getObservationDate() != null) {
			o.setEffective(new DateTimeType(om.getObservationDate()));
		}
		/*
		 * OBX 5, observation Value , depending on OBX-2 value
		 */
		Type value = getObservationReportedValue(om);
		o.setValue(value);
		/*
		 * OBX-6 unit levels is dealt with in Quantity methods
		 */
		/*
		 * OBX-11
		 */
		Observation.ObservationStatus observationStatus;
		switch (om.getResultStatus()) {
			case "A": {
				observationStatus = Observation.ObservationStatus.AMENDED;
				break;
			}
			case "C": {
				observationStatus = Observation.ObservationStatus.CORRECTED;
				break;
			}
			case "D": {
				observationStatus = Observation.ObservationStatus.ENTEREDINERROR;
				break;
			}
			case "F": {
				observationStatus = Observation.ObservationStatus.FINAL;
				break;
			}
			case "P": {
				observationStatus = Observation.ObservationStatus.PRELIMINARY;
				break;
			}
			case "X": {
				Extension extension = o.addExtension().setUrl("http://hl7.org/fhir/StructureDefinition/alternate-codes");
				CodeableConcept codeableConcept = new CodeableConcept();
				codeableConcept.addCoding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0085").setCode("X");
				extension.setValue(codeableConcept);
				observationStatus = Observation.ObservationStatus.CANCELLED;
				break;
			}
			case "N": {
				o.setDataAbsentReason(new CodeableConcept(new Coding().setCode("not-asked").setSystem("http://terminology.hl7.org/CodeSystem/data-absent-reason")));
				observationStatus = Observation.ObservationStatus.UNKNOWN;
				break;
			}
			case "B":
			case "I":
			case "O":
			case "R":
			case "S":
			case "V":
			case "U":
			case "W":
			default: {
				observationStatus = Observation.ObservationStatus.NULL;
				break;
			}
		}
		o.setStatus(observationStatus);
		if (om.getResultStatus() != null) {
			o.addExtension()
				.setUrl(V_2_STATUS_EXTENSION)
				.setValue(new Coding().setCode(om.getResultStatus()).setSystem("HL70085"));
		}
		/*
		 * OBX-21
		 */
		for (BusinessIdentifier businessIdentifier : om.getBusinessIdentifiers()) {
			o.addIdentifier(businessIdentifier.toR4());
		}
		/*
		 * Components , other OBX with same subId
		 */
		for (ObservationMaster component : om.getComponents()) {
			Observation.ObservationComponentComponent observationComponent = fhirObservationComponent(component);
			o.addComponent(observationComponent);
		}
		return o;
	}

	private static Observation.ObservationComponentComponent fhirObservationComponent(ObservationMaster component) {
		Observation.ObservationComponentComponent observationComponent = new Observation.ObservationComponentComponent();
		/*
		 * OBX-2 extension Value type
		 */
		if (component.getValueType() != null) {
			observationComponent.addExtension().setUrl(OBS_TYPE_OBX_2).setValue(new StringType(component.getValueType()));
		}
		/*
		 * OBX-3
		 */
		observationComponent.setCode(getValueCodeCodeableConcept(component));
		/*
		 * OBX-5 & OBX-6
		 */
		observationComponent.setValue(getObservationReportedValue(component));
		/*
		 * OBX-11
		 */
		if (component.getResultStatus() != null) {
			observationComponent.addExtension()
				.setUrl(V_2_STATUS_EXTENSION)
				.setValue(new Coding().setCode(component.getResultStatus()).setSystem("HL70085"));
		}
		return observationComponent;
	}

	public ObservationReported localObjectReported(Observation o) {
		ObservationReported observationReported = new ObservationReported();
		/*
		 * Id
		 */
		observationReported.setObservationId(StringUtils.defaultString(o.getId()));
		/*
		 * Updated Date
		 */
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		/*
		 * Reported Date
		 */
		{
			Extension recorded = o.getExtensionByUrl(ImmunizationMapper.RECORDED);
			if (recorded != null) {
				observationReported.setReportedDate(MappingHelper.extensionGetDate(recorded));
			} else {
				observationReported.setReportedDate(null);
			}
		}
		/*
		 * Observation Date
		 */
		if (o.hasEffectiveDateTimeType()) {
			observationReported.setObservationDate(o.getEffectiveDateTimeType().getValue());
		}
		/*
		 * Vaccination Part of
		 */
		Reference vaccinationReference = o.getPartOf().stream().filter(ref -> ref.getReference().startsWith("Immunization/")).findFirst().orElse(null);
		if (vaccinationReference != null) {
			observationReported.setVaccinationReportedId(vaccinationReference.getReferenceElement().getIdPart());
		}
		/*
		 * Observation member
		 */
		Reference observationReference = o.getHasMember().stream().filter(ref -> ref.getReference().startsWith(OBSERVATION + "/")).findFirst().orElse(null);
		if (observationReference != null) {
			observationReported.setPartOfObservationId(observationReference.getReferenceElement().getIdPart());
		}
		/*
		 * Patient subject
		 */
		Reference patientReference = o.getSubject();
		if (patientReference != null) {
			observationReported.setPatientReportedId(patientReference.getReferenceElement().getIdPart());
		}
		/*
		 * Value type
		 */
		{
			Extension obx2 = o.getExtensionByUrl(OBS_TYPE_OBX_2);
			if (obx2 != null) {
				observationReported.setValueType(MappingHelper.extensionGetString(obx2));
			} else {
				observationReported.setValueType(null);
			}
		}
		/*
		 * OBX-3 Observation code / Identifier
		 */
		if (o.hasCode() && o.getCode().hasCoding()) {
			observationReported.setIdentifierCode(o.getCode().getCodingFirstRep().getCode());
			observationReported.setIdentifierLabel(o.getCode().getCodingFirstRep().getDisplay());
			observationReported.setIdentifierTable(o.getCode().getCodingFirstRep().getSystem());
		}
		/*
		 * OBX-5 Value , OBX 6 Unit when Quantity are available
		 */
		if (o.hasValueCodeableConcept() && o.getValueCodeableConcept().hasCoding()) {
			observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
			observationReported.setValueLabel(StringUtils.defaultString(o.getValueCodeableConcept().getCodingFirstRep().getDisplay()));
			observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		} else if (o.hasValueQuantity()) {
			Quantity quantity = o.getValueQuantity();
//			observationReported.setUnitsCode(quantity.getUnit()); // TODO get from codemap ?
			observationReported.setUnitsTable(quantity.getSystem());
			observationReported.setUnitsLabel(quantity.getUnit());
		} else if (o.hasValueDateTimeType()) {
			SimpleDateFormat simpleDateFormat = IncomingMessageHandler.getV2SDF();
			observationReported.setValueCode(simpleDateFormat.format(o.getValueDateTimeType().getValue()));
		}
		/*
		 * OBX 17 method
		 */
		if (o.hasMethod() && o.getMethod().hasCoding()) {
			observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
			observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
			observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());
		}
		/*
		 * status
		 */
		Extension status = o.getExtensionByUrl(V_2_STATUS_EXTENSION);
		if (status != null && status.hasValue()) {
			observationReported.setResultStatus(MappingHelper.extensionGetCoding(status).getCode());
		}
		/*
		 * Identifiers
		 */
		for (Identifier identifier : o.getIdentifier()) {
			observationReported.addBusinessIdentifier(BusinessIdentifier.fromR4(identifier));
		}
		/*
		 * Components
		 */
		for (Observation.ObservationComponentComponent observationComponent : o.getComponent()) {
			ObservationMaster component = fromFhirComponent(observationComponent);
			component.setVaccinationReportedId(observationReported.getVaccinationReportedId());
			component.setPatientReportedId(observationReported.getPatientReportedId());
			component.setUpdatedDate(observationReported.getUpdatedDate());
			component.setReportedDate(observationReported.getReportedDate());
			component.setObservationDate(observationReported.getObservationDate());
			observationReported.addComponent(component);
		}
		return observationReported;
	}

	private static @NotNull ObservationMaster fromFhirComponent(Observation.ObservationComponentComponent observationComponent) {
		ObservationMaster component = new ObservationMaster();
		/*
		 * Value type
		 */
		{
			String valueType = "";
			Extension obx2 = observationComponent.getExtensionByUrl(OBS_TYPE_OBX_2);
			if (obx2 != null) {
				valueType = MappingHelper.extensionGetString(obx2);
				component.setValueType(valueType);
			} else {
				component.setValueType(null);
			}
		}
		/*
		 * OBX-3 Observation code / Identifier
		 */
		if (observationComponent.hasCode() && observationComponent.getCode().hasCoding()) {
			component.setIdentifierCode(observationComponent.getCode().getCodingFirstRep().getCode());
			component.setIdentifierLabel(observationComponent.getCode().getCodingFirstRep().getDisplay());
			component.setIdentifierTable(observationComponent.getCode().getCodingFirstRep().getSystem());
		}
		/*
		 * OBX-5 Value , OBX 6 Unit when Quantity are available
		 */
		if (observationComponent.hasValueCodeableConcept() && observationComponent.getValueCodeableConcept().hasCoding()) {
			component.setValueCode(observationComponent.getValueCodeableConcept().getCodingFirstRep().getCode());
			component.setValueLabel(observationComponent.getValueCodeableConcept().getCodingFirstRep().getDisplay());
			component.setValueTable(observationComponent.getValueCodeableConcept().getCodingFirstRep().getSystem());
		} else if (observationComponent.hasValueQuantity()) {
			Quantity quantity = observationComponent.getValueQuantity();
//			component.setUnitsCode(quantity.getUnit()); // TODO get from codemap ?
			component.setUnitsTable(quantity.getSystem());
			component.setUnitsLabel(quantity.getUnit());
		} else if (observationComponent.hasValueDateTimeType()) {
			SimpleDateFormat simpleDateFormat = IncomingMessageHandler.getV2SDF();
			component.setValueCode(simpleDateFormat.format(observationComponent.getValueDateTimeType().getValue()));
		}
		/*
		 * status
		 */
		Extension status = observationComponent.getExtensionByUrl(V_2_STATUS_EXTENSION);
		if (status != null && status.hasValue()) {
			component.setResultStatus(MappingHelper.extensionGetCoding(status).getCode());
		}
		return component;
	}

	private static @NotNull Range valueRange(ObservationMaster om) {
		Range range = new Range();
		Quantity low = valueQuantity(om.getValueCode(), om);
		if (low.getValue() != null) {
			range.setLow(low);
		}
		Quantity high = valueQuantity(om.getValueLabel(), om);
		if (high.getValue() != null) {
			range.setHigh(high);
		}
		return range;
	}

	private static @NotNull DateTimeType valueDateTimeType(ObservationMaster om) {
		SimpleDateFormat simpleDateFormat = IncomingMessageHandler.getV2SDF();
		DateTimeType dateTimeType = new DateTimeType();
		try {
			dateTimeType.setValue(simpleDateFormat.parse(om.getValueCode()));
		} catch (ParseException ignored) {
		}
		return dateTimeType;
	}

	private static @NotNull Period valuePeriod(ObservationMaster om) {
		SimpleDateFormat simpleDateFormat = IncomingMessageHandler.getV2SDF();
		Period period = new Period();
		try {
			Date start = simpleDateFormat.parse(om.getValueCode());
			period.setStart(start);
		} catch (ParseException ignored) {
		}
		try {
			Date end = simpleDateFormat.parse(om.getValueLabel());
			period.setEnd(end);
		} catch (ParseException ignored) {
		}
		return period;
	}

	private static CodeableConcept valueCodeableConcept(ObservationMaster om) {
		Coding valueCoding = new Coding();
		valueCoding.setCode(om.getValueCode());
		valueCoding.setDisplay(om.getValueLabel());
		valueCoding.setSystem(om.getValueTable());
		return new CodeableConcept().addCoding(valueCoding);
	}

	private static Quantity valueQuantity(String value, ObservationMaster om) {
		Quantity quantity = new Quantity();
		if (NumberUtils.isCreatable(value)) {
			quantity.setValue(NumberUtils.createDouble(value));
		}
		if (StringUtils.isNotBlank(om.getUnitsLabel())) {
			quantity.setUnit(om.getUnitsLabel());
		} else {
			quantity.setUnit(StringUtils.defaultString(om.getUnitsCode()));
		}
		if (StringUtils.isNotBlank(om.getUnitsCode()) && StringUtils.isNotBlank(om.getUnitsLabel())) {
			quantity.setSystem(om.getUnitsTable());
		}
		return quantity;
	}

	private static @Nullable Type getObservationReportedValue(ObservationMaster om) {
		Type value = null;
		switch (om.getValueType()) {
			case "NM": {
				value = valueQuantity(om.getValueCode(), om);
				break;
			}
			case "CF":
			case "CNE":
			case "CE":
			case "CWE":
			case "IS": {
				value = valueCodeableConcept(om);
				break;
			}
			case "DR": {
				value = valuePeriod(om);
				break;
			}
			case "DTM":
			case "TS": // TODO signal issue in V2-to-FHIR IG
			case "DT": {
				value = valueDateTimeType(om);
				break;
			}
			case "NR": {
				value = valueRange(om);
				break;
			}
			case "TM": {
				value = new TimeType().setValue(om.getValueCode());
				break;
			}
			case "SN": {
				if ("<>".equals(om.getValueCode())) { // TODO complete
					value = new StringType().setValue(om.getValueCode());
				} else if (":".equals(om.getValueTable()) || "\\".equals(om.getValueTable())) {
					Ratio ratio = new Ratio();
					Quantity numerator = valueQuantity(om.getValueLabel(), om);
					numerator.setComparator(Quantity.QuantityComparator.valueOf(om.getValueCode()));
					ratio.setNumerator(numerator);
//					ratio.setDenominator(); NOT SUPPORTED TODO
					value = ratio;
				} else if ("-".equals(om.getValueTable())) {

				} else if ("+".equals(om.getValueTable())) {
				}

				break;
			}
			case "NA":
			case "ED":
			case "EI":
			case "RP": {
				break;
			}
			case "ST":
			case "FT":
			case "TX":
			case "VR":
			default: {
				value = new StringType().setValue(om.getValueCode());
				break;
			}
		}
		return value;
	}

	private static CodeableConcept getValueCodeCodeableConcept(ObservationMaster om) {
		boolean codeNonNull = false;
		Coding observationCodeCoding = new Coding();
		if (om.getIdentifierCode() != null) {
			observationCodeCoding.setCode(om.getIdentifierCode());
			codeNonNull = true;
		}
		if (om.getIdentifierLabel() != null) {
			observationCodeCoding.setDisplay(om.getIdentifierLabel());
			codeNonNull = true;
		}
		if (om.getIdentifierTable() != null) {
			observationCodeCoding.setSystem(om.getIdentifierTable());
			codeNonNull = true;
		}
		CodeableConcept codeableConcept = null;
		if (codeNonNull) {
			codeableConcept = new CodeableConcept(observationCodeCoding);
		}
		return codeableConcept;
	}

}
