package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.IncomingMessageHandler;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
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
		if (!om.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new org.hl7.fhir.r4.model.Reference().setReference(IMMUNIZATION + "/" + om.getVaccinationReportedId()));
		}
		if (om.getPatientReportedId() != null) {
			o.setSubject(new org.hl7.fhir.r4.model.Reference().setReference(PATIENT + "/" + om.getPatientReportedId()));
//				o.setSubject(MappingHelper.getFhirReference(PATIENT,PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
		}
		o.setValue(new org.hl7.fhir.r4.model.CodeableConcept(new org.hl7.fhir.r4.model.Coding()
			.setCode(om.getValueCode())
			.setSystem(om.getValueTable())
			.setDisplay(om.getValueLabel())));
		o.setMethod(new org.hl7.fhir.r4.model.CodeableConcept()).getMethod().addCoding()
			.setCode(om.getMethodCode())
			.setSystem(om.getMethodTable())
			.setDisplay(om.getMethodLabel());
		o.addIdentifier(MappingHelper.getFhirIdentifierR4(
			om.getIdentifierTable(), om.getIdentifierCode())); //TODO label
		o.addComponent().setValue(new org.hl7.fhir.r4.model.DateTimeType(om.getObservationDate()))
			.setCode(new org.hl7.fhir.r4.model.CodeableConcept().setText(OBSERVATION_DATE));
		o.addReferenceRange().setText(om.getUnitsLabel())
			.addAppliesTo().setText(om.getUnitsTable())
			.addCoding().setCode(om.getUnitsCode());
		o.addInterpretation().setText(RESULT_STATUS)
			.addCoding().setCode(om.getResultStatus());

		SimpleDateFormat simpleDateFormat = IncomingMessageHandler.getV2SDF();

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
				Period period = valuePeriod(om, simpleDateFormat);
				o.setValue(period);
				break;
			}
			case "DTM":
			case "DT": {
				DateTimeType dateTimeType = valueDateTimeType(om, simpleDateFormat);
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

	private static @NotNull DateTimeType valueDateTimeType(ObservationMaster om, SimpleDateFormat simpleDateFormat) {
		DateTimeType dateTimeType = new DateTimeType();
		try {
			dateTimeType.setValue(simpleDateFormat.parse(om.getValueCode()));
		} catch (ParseException ignored) {
		}
		return dateTimeType;
	}

	private static @NotNull Period valuePeriod(ObservationMaster om, SimpleDateFormat simpleDateFormat) {
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
