package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.immregistries.iis.kernal.mapping.MappingHelper.IMMUNIZATION;
import static org.immregistries.iis.kernal.mapping.MappingHelper.PATIENT;

@Service
@Conditional(OnR4Condition.class)
public class ObservationMapperR4 implements ObservationMapper<Observation> {
	public static final String V_2_STATUS_EXTENSION = "v2Status";
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
		o.setId(StringUtils.defaultString(om.getObservationId()));
		/*
		 * Updated Date
		 */
		o.getMeta().setLastUpdated(om.getUpdatedDate());
		/*
		 * Recorded Date
		 */
		if (om.getReportedDate() != null) {
			o.addExtension()
				.setUrl(ImmunizationMapper.RECORDED)
				.setValue(new DateType(om.getReportedDate()));
		}
		//TODO reported Date
		/*
		 * OBX-2 extension Value type
		 */
		Extension oType = o.addExtension().setUrl("ObsType-OBX-2").setValue(new StringType(om.getValueType()));
		/*
		 * Part of Vaccination
		 */
		if (!om.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new Reference().setReference(IMMUNIZATION + "/" + om.getVaccinationReportedId()));
		}
		/*
		 * Patient/Subject
		 */
		if (om.getPatientReportedId() != null) {
			o.setSubject(new Reference().setReference(PATIENT + "/" + om.getPatientReportedId()));
		}
		/*
		 * OBX-3 observation code
		 */
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
		if (codeNonNull) {
			o.setCode(new CodeableConcept().addCoding(observationCodeCoding));
		}
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
			observationMethodCoding.setSystem(om.getMethodCode());
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
		switch (om.getValueType()) {
			case "NM": {
				o.setValue(valueQuantity(om.getValueCode(), om));
				break;
			}
			case "ST":
			case "FT":
			case "TX":
			case "VR": {
				o.setValue(new StringType().setValue(om.getValueCode()));
				break;
			}
			case "CF":
			case "CNE":
			case "CE":
			case "CWE":
			case "IS": {
				CodeableConcept value = valueCodeableConcept(om);
				o.setValue(value);
				break;
			}
			case "DR": {
				Period period = valuePeriod(om);
				o.setValue(period);
				break;
			}
			case "DTM":
			case "DT": {
				DateTimeType dateTimeType = valueDateTimeType(om);
				o.setValue(dateTimeType);
				break;
			}
			case "NR": {
				Range range = valueRange(om);
				o.setValue(range);

				break;
			}
			case "TM": {
				o.setValue(new TimeType().setValue(om.getValueCode()));
				break;
			}
			case "SN": {
				if ("<>".equals(om.getValueCode())) { // TODO complete
					o.setValue(new StringType().setValue(om.getValueCode()));
				} else if (":".equals(om.getValueTable()) || "\\".equals(om.getValueTable())) {
					Ratio ratio = new Ratio();
					Quantity numerator = valueQuantity(om.getValueLabel(), om);
					numerator.setComparator(Quantity.QuantityComparator.valueOf(om.getValueCode()));
					ratio.setNumerator(numerator);
//					ratio.setDenominator(); NOT SUPPORTED TODO
					o.setValue(ratio);
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
		}
		/*
		 * OBX-6 unit levels is dealt with in Quantity methods
		 */
		o.addReferenceRange().setText(om.getUnitsLabel())
			.addAppliesTo().setText(om.getUnitsTable())
			.addCoding().setCode(om.getUnitsCode());
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
		o.addExtension()
			.setUrl(V_2_STATUS_EXTENSION)
			.setValue(new Coding().setCode(om.getResultStatus()).setSystem("HL70085"));

		for (BusinessIdentifier businessIdentifier : om.getBusinessIdentifiers()) {
			o.addIdentifier(businessIdentifier.toR4());
		}
		return o;
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
		CodeableConcept value = new CodeableConcept().addCoding(valueCoding);
		return value;
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

	public ObservationReported localObjectReported(Observation o) {
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		if (o.hasEffectiveDateTimeType()) {
			observationReported.setReportedDate(o.getEffectiveDateTimeType().getValue());
		}
		observationReported.setObservationId(StringUtils.defaultString(o.getId()));
//		observationReported.setVaccinationReportedId(o.getPartOfFirstRep().getId());
		observationReported.setVaccinationReportedId(StringUtils.defaultString(o.getPartOf().stream().filter(ref -> ref.getReference().startsWith("Immunization/")).findFirst().orElse(new Reference("")).getId()));
		observationReported.setPatientReportedId(StringUtils.defaultString(o.getSubject().getId()));
		if (o.getValueCodeableConcept().hasCoding()) {
			observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
			observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
			observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		}
		if (o.getMethod().hasCoding()) {
			observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
			observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
			observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());
		}
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




}
