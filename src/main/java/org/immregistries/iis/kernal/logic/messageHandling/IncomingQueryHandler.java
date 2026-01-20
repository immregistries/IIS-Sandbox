package org.immregistries.iis.kernal.logic.messageHandling;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.*;
import org.immregistries.iis.kernal.logic.ack.IisHL7Util;
import org.immregistries.iis.kernal.logic.ack.V2DateParseService;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.logic.ack.ReportableUtil;
import org.immregistries.iis.kernal.mapping.requesters.FhirMatchRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.logic.IIncomingMessageHandler.*;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class IncomingQueryHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	FhirSearchRequester fhirSearchRequester;
	@Autowired
	FhirMatchRequester fhirMatchRequester;
	@Autowired
	Hl7MessageWriter hl7MessageWriter;
	@Autowired
	ValidationService validationService;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	IisHL7Util iisHL7Util;
	@Autowired
	MessageRecordingService messageRecordingService;
	@Autowired
	ReportableUtil reportableUtil;
	@Autowired
	V2DateParseService v2DateParseService;

	public String processQBP(Tenant tenant, HL7Reader reader, String messageReceived, IIdType managingOrganizationId) throws Exception {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = validationService.getMqeMessageService().processMessage(messageReceived);
		List<IisReportable> reportables = validationService.nistValidation(messageReceived, mqeMessageServiceResponse.getMessageObjects().getMessageHeader().getMessageProfile());
		IisPatient patientForMatchQuery = new IisPatient();
		if (reader.advanceToSegment("QPD")) {
			String mrn = "";
			String mrnType = "";
			{
				mrnType = BusinessIdentifier.MRN_TYPE_VALUE;
				mrn = reader.getValueBySearchingRepeats(3, 1, mrnType, 5);
				if (StringUtils.isBlank(mrn)) {
					mrnType = BusinessIdentifier.PT_TYPE_VALUE;
					mrn = reader.getValueBySearchingRepeats(3, 1, mrnType, 5);
				}
			}
			String problem = null;
			int fieldPosition = 0;
			if (StringUtils.isNotBlank(mrn)) {
				BusinessIdentifier businessIdentifier = new BusinessIdentifier();
				businessIdentifier.setValue(mrn);
				businessIdentifier.setType(BusinessIdentifier.MRN_TYPE_VALUE);
				patientForMatchQuery.addBusinessIdentifier(businessIdentifier);
//				patientReported = fhirSearchRequester.searchPatientReported(
//					Patient.IDENTIFIER.exactly().systemAndCode(MRN_SYSTEM, mrn)
//				);
			}
			String patientNameLast = reader.getValue(4, 1);
			String patientNameFirst = reader.getValue(4, 2);
			String patientNameMiddle = reader.getValue(4, 3);

			if (processingFlavorSet.contains(ProcessingFlavor.MOONFRUIT) && (StringUtils.defaultString(patientNameFirst).startsWith("S") || StringUtils.defaultString(patientNameFirst).startsWith("A"))) {
				throw new ProcessingException("Immunization History cannot be Accepted because of patient's consent status", "PID", 0, 0, IisReportableSeverity.WARN);
			}
			boolean strictDate = false;

			Date patientBirthDate = v2DateParseService.parseDateWarn(reader.getValue(6), "Invalid patient birth date", "QPD", 1, 6, strictDate, reportables);
			String patientSex = reader.getValue(7);

			if (StringUtils.isBlank(patientNameLast)) {
				problem = "Last name is missing";
				fieldPosition = 4;
			} else if (StringUtils.isBlank(patientNameFirst)) {
				problem = "First name is missing";
				fieldPosition = 4;
			} else if (patientBirthDate == null) {
				problem = "Date of Birth is missing";
				fieldPosition = 6;
			}
			if (StringUtils.isNotBlank(problem)) {
				reportables.add(reportableUtil.fromProcessingException(new ProcessingException(problem, "QPD", 1, fieldPosition)));
			} else {
				ModelName modelName = new ModelName(patientNameLast, patientNameFirst, patientNameMiddle, "");
				patientForMatchQuery.addPatientName(modelName);
				patientForMatchQuery.setBirthDate(patientBirthDate);
			}
		} else {
			reportables.add(reportableUtil.fromProcessingException(new ProcessingException("QPD segment not found", null, 0, 0)));
		}

		Date cutoff = null;
		if (processingFlavorSet.contains(ProcessingFlavor.SNAIL) || processingFlavorSet.contains(ProcessingFlavor.SNAIL30) || processingFlavorSet.contains(ProcessingFlavor.SNAIL60) || processingFlavorSet.contains(ProcessingFlavor.SNAIL90)) {
			Calendar calendar = Calendar.getInstance();
			int seconds = -30;
			if (processingFlavorSet.contains(ProcessingFlavor.SNAIL30)) {
				seconds = -30;
			} else if (processingFlavorSet.contains(ProcessingFlavor.SNAIL60)) {
				seconds = -60;
			} else if (processingFlavorSet.contains(ProcessingFlavor.SNAIL90)) {
				seconds = -90;
			} else {
				int delay = calendar.get(Calendar.MINUTE) % 4;
				seconds = delay * -30;
			}

			calendar.add(Calendar.SECOND, seconds);
			cutoff = calendar.getTime();
		}
		List<PatientReported> multipleMatches = new ArrayList<>();
		PatientMaster singleMatch = fhirMatchRequester.matchPatient(multipleMatches, patientForMatchQuery, cutoff);
		if (singleMatch == null) {
			throw new ProcessingException("Patient not found", "PID", 1, 1); // TODO position
		}

		return buildRSP(reader, messageReceived, singleMatch, tenant, multipleMatches, reportables, managingOrganizationId);
	}

	public String buildRSP(HL7Reader reader, String messageReceived, PatientMaster patientMaster, Tenant tenant, List<PatientReported> patientReportedPossibleList, List<IisReportable> iisReportables, IIdType managingOrganizationId) {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = validationService.getMqeMessageService().processMessage(messageReceived);
		boolean sendInformations = true;
		if (processingFlavorSet.contains(ProcessingFlavor.STARFRUIT) && (StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("S") || StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("A"))) {
			iisReportables.add(reportableUtil.fromProcessingException(new ProcessingException("Immunization History cannot be shared because of patient's consent status", "PID", 0, 0, IisReportableSeverity.NOTICE)));
			sendInformations = false;
		}
		reader.resetPostion();
		reader.advanceToSegment("MSH");

		StringBuilder sb = new StringBuilder();
		String profileIdSubmitted = reader.getValue(21);
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		String categoryResponse = NO_MATCH;
		String profileId = RSP_Z33_NO_MATCH;
		boolean sendBackForecast = true;
		if (processingFlavorSet.contains(ProcessingFlavor.COCONUT)) {
			sendBackForecast = false;
		} else if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
			sendBackForecast = false;
		}

		String queryId = "";
		int maxCount = 20;
		if (reader.advanceToSegment("QPD")) {
			queryId = reader.getValue(2);
			if (reader.advanceToSegment("RCP")) {
				String s = reader.getValue(2);
				try {
					int i = Integer.parseInt(s);
					if (i < maxCount) {
						maxCount = i;
					}
				} catch (NumberFormatException nfe) {
					// ignore
				}
			}
		}
		String queryResponse = QUERY_OK;
		{
			if (patientMaster == null) {
				queryResponse = QUERY_NOT_FOUND;
				profileId = RSP_Z33_NO_MATCH;
				categoryResponse = NO_MATCH;
				if (!patientReportedPossibleList.isEmpty()) {
					if (profileIdSubmitted.equals(QBP_Z34)) {
						if (patientReportedPossibleList.size() > maxCount) {
							queryResponse = QUERY_TOO_MANY;
							profileId = RSP_Z33_NO_MATCH;
							categoryResponse = TOO_MANY_MATCHES;
						} else {
							queryResponse = QUERY_OK;
							profileId = RSP_Z31_MULTIPLE_MATCH;
							categoryResponse = POSSIBLE_MATCH;
						}
					} else if (profileIdSubmitted.equals("Z44")) {
						queryResponse = QUERY_NOT_FOUND;
						profileId = RSP_Z33_NO_MATCH;
						categoryResponse = NO_MATCH;
					}
				}
				if (IIncomingMessageHandler.hasErrors(iisReportables)) {
					queryResponse = QUERY_APPLICATION_ERROR;
				}
			} else if (profileIdSubmitted.equals(QBP_Z34)) {
				profileId = RSP_Z32_MATCH;
				categoryResponse = MATCH;
			} else if (profileIdSubmitted.equals(QBP_Z44)) {
				if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
					profileId = RSP_Z32_MATCH;
					categoryResponse = MATCH;
				} else {
					sendBackForecast = true;
					profileId = RSP_Z42_MATCH_WITH_FORECAST;
					categoryResponse = MATCH;
				}
			} else {
				iisReportables.add(reportableUtil.fromProcessingException(new ProcessingException("Unrecognized profile id '" + profileIdSubmitted + "'", "MSH", 1, 21)));
			}
			// TODO remove notices ?
			hl7MessageWriter.createMSH(RSP_K_11_RSP_K_11, profileId, reader, sb, processingFlavorSet);
		}

		{
			String sendersUniqueId = reader.getValue(10);
			String processingId = mqeMessageServiceResponse.getMessageObjects().getMessageHeader().getProcessingStatus();
			iisHL7Util.makeMsaAndErr(sb, sendersUniqueId, processingId, profileId, iisReportables, processingFlavorSet);
		}

		if (sendInformations) {
			String profileName = "Request a Complete Immunization History";
			if (StringUtils.isBlank(profileIdSubmitted)) {
				profileIdSubmitted = "Z34";
				profileName = "Request a Complete Immunization History";
			} else if (profileIdSubmitted.equals("Z34")) {
				profileName = "Request a Complete Immunization History";
			} else if (profileIdSubmitted.equals("Z44")) {
				profileName = "Request Evaluated Immunization History and Forecast Query";
			}
			{
				sb.append("QAK|").append(queryId);
				sb.append("|").append(queryResponse);
				sb.append("|");
				sb.append(profileIdSubmitted).append("^").append(profileName).append("^CDCPHINVS\r");
			}
			reader.resetPostion();
			if (reader.advanceToSegment("QPD")) {
				sb.append(reader.getOriginalSegment()).append("\r");
			} else {
				sb.append("QPD|");
			}
			if (profileId.equals(RSP_Z31_MULTIPLE_MATCH)) {
				SimpleDateFormat sdf = v2DateParseService.generateSimpleDateFormat();
				int count = 0;
				for (PatientReported pr : patientReportedPossibleList) {
					count++;
					PatientMaster patient = pr.getMasterRecord();
					hl7MessageWriter.printQueryPID(pr, processingFlavorSet, sb, patient, sdf, count);
				}
			} else if (profileId.equals(RSP_Z32_MATCH) || profileId.equals(RSP_Z42_MATCH_WITH_FORECAST)) {
				/**
				 * CONFUSING naming p but no better solution right now but to deal with single match
				 */
				IisPatient matchedPatient = patientMaster;
				SimpleDateFormat sdf = v2DateParseService.generateSimpleDateFormat();
				hl7MessageWriter.printQueryPID(matchedPatient, processingFlavorSet, sb, patientMaster, sdf, 1);
				if (profileId.equals(RSP_Z32_MATCH)) {
					hl7MessageWriter.printQueryNK1(patientMaster, sb, codeMap);
				}

				List<VaccinationMaster> vaccinationMasterList = fhirSearchRequester.searchVaccinationMasterGoldenList(
					new SearchParameterMap("patient", new ReferenceParam().setMdmExpand(true).setValue("Patient/" + patientMaster.getPatientId())));
				vaccinationMasterList.sort(Comparator.comparing(VaccinationMaster::getAdministeredDate));


				if (processingFlavorSet.contains(ProcessingFlavor.LEMON)) {
					for (Iterator<VaccinationMaster> it = vaccinationMasterList.iterator(); it.hasNext(); ) {
						it.next();
						if (random.nextInt(4) == 0) {
							it.remove();
						}
					}
				}
				if (processingFlavorSet.contains(ProcessingFlavor.GREEN)) {
					vaccinationMasterList.removeIf(vaccinationMaster -> "91".equals(vaccinationMaster.getVaccineCvxCode()));
				}
				List<ForecastActual> forecastActualList = null;
				if (sendBackForecast) {
					forecastActualList = doForecast(patientMaster, vaccinationMasterList, tenant, new Date());
				}

				int obxSetId = 0;
				int obsSubId = 0;
				for (VaccinationMaster vaccination : vaccinationMasterList) {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
					if (cvxCode == null) {
						continue;
					}

					boolean originalReporter = vaccination.getPatientReported().getManagingOrganizationId().equals(managingOrganizationId.getValue()); // TODO verify
					if ("D".equals(vaccination.getActionCode())) {
						continue;
					}
					hl7MessageWriter.printORC(tenant, sb, vaccination, originalReporter);
					printRXA(vaccination, sb, obxSetId, processingFlavorSet, cvxCode);
					if (StringUtils.isNotBlank(vaccination.getBodyRoute())) {
						printRXR(vaccination, sb);
					}
					TestEvent testEvent = vaccination.getTestEvent();
					if (testEvent != null && testEvent.getEvaluationActualList() != null) {
						HashSet<String> cvxEvaluatedSet = new HashSet<>();
						for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
//							logger.info("CVX {}, testEvent cvx {}", cvxCode.getLabel(), evaluationActual.getVaccineCvx());
							String cvx = evaluationActual.getVaccineCvx();
							if (cvxEvaluatedSet.contains(cvx)) {
								continue;
							}
							cvxEvaluatedSet.add(cvx);
							obsSubId++;
							{
								obxSetId++;
								String loinc = "30956-7";
								String loincLabel = "Vaccine type";
								String value = evaluationActual.getVaccineCvx();
								if (processingFlavorSet.contains(ProcessingFlavor.KUMQUAT)) {
									if (value.length() > 2) {
										value = "BADCVX";
									}
								}
								Code code = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, evaluationActual.getVaccineCvx());

								String valueLabel;
								if (code != null) {
									valueLabel = code.getLabel();
								} else {
									valueLabel = evaluationActual.getVaccineCvx();
								}
								String valueTable = "CVX";
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
							}
							{
								obxSetId++;
								String loinc = "59781-5";
								String loincLabel = "Dose validity";
								String value = evaluationActual.getDoseValid();
								String valueLabel = value;
								String valueTable = "99107";
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
							}
							if ("N".equals(evaluationActual.getDoseValid())) {
								obxSetId++;
								String loinc = "30982-3";
								String loincLabel = "Reason for validity";
								String value = evaluationActual.getReasonCode();
								String valueLabel = evaluationActual.getReasonText();
								String valueTable = "99107";
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
							}
						}
					}


					List<ObservationReported> observationVaccinationList = fhirSearchRequester.searchObservationReportedList(
						new SearchParameterMap("part-of", new ReferenceParam().setMdmExpand(true).setValue("Immunization/" + vaccination.getVaccinationId())));

					for (ObservationMaster observationMaster : observationVaccinationList) {
						obxSetId++;
						obsSubId++;
						hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationMaster);
						for (ObservationMaster sub : observationMaster.getComponents()) {
							obxSetId++;
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, sub);
						}
					}
				}

				List<ObservationReported> observationReportedPatientList = fhirSearchRequester.searchObservationReportedList(
					new SearchParameterMap("subject", new ReferenceParam().setMdmExpand(true).setValue("Patient/" + patientMaster.getPatientId()))
						.add("part-of", new ReferenceParam().setMissing(true))
				);
				for (ObservationMaster observationMaster : observationReportedPatientList) {
					obxSetId++;
					obsSubId++;
					hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationMaster);
					for (ObservationMaster sub : observationMaster.getComponents()) {
						obxSetId++;
						hl7MessageWriter.printObx(sb, obxSetId, obsSubId, sub);
					}
				}

				if (sendBackForecast && forecastActualList != null && !forecastActualList.isEmpty()) {
					hl7MessageWriter.printORC(tenant, sb, null, false);
					sb.append("RXA");
					// RXA-1
					sb.append("|0");
					// RXA-2
					sb.append("|1");
					// RXA-3
					sb.append("|" + sdf.format(new Date()));
					// RXA-4
					sb.append("|");
					// RXA-5
					sb.append("|998^No Vaccination Administered^CVX");
					// RXA-6
					sb.append("|999");
					{
						// RXA-7
						sb.append("|");
						// RXA-8
						sb.append("|");
						// RXA-9
						sb.append("|");
						// RXA-10
						sb.append("|");
						// RXA-11
						sb.append("|");
						// RXA-12
						sb.append("|");
						// RXA-13
						sb.append("|");
						// RXA-14
						sb.append("|");
						// RXA-15
						sb.append("|");
						// RXA-16
						sb.append("|");
						// RXA-17
						sb.append("|");
						// RXA-18
						sb.append("|");
						// RXA-19
						sb.append("|");
						// RXA-20
					}
					sb.append("|NA");
					sb.append("\r");
					HashSet<String> cvxAddedSet = new HashSet<>();
					for (ForecastActual forecastActual : forecastActualList) {
						String cvx = forecastActual.getVaccineGroup().getVaccineCvx();
						if (cvxAddedSet.contains(cvx)) {
							continue;
						}
						cvxAddedSet.add(cvx);
						obsSubId++;
						{
							obxSetId++;
							String loinc = "30956-7";
							String loincLabel = "Vaccine type";
							String value = forecastActual.getVaccineGroup().getVaccineCvx();
							String valueLabel = forecastActual.getVaccineGroup().getLabel();
							String valueTable = "CVX";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59783-1";
							String loincLabel = "Status in immunization series";
							VaccinePlanStatus vaccinePlanStatus = VaccinePlanStatus.fromForecastActual(forecastActual);
							String value;
							String valueLabel;
							String valueTable;
							if (vaccinePlanStatus != null) {
								value = vaccinePlanStatus.getCode();
								valueLabel = vaccinePlanStatus.getLabel();
								valueTable = vaccinePlanStatus.getTable();
							} else {
								Admin admin = forecastActual.getAdmin();
								value = admin.getAdminStatus();
								valueLabel = admin.getLabel();
								valueTable = "99106";
							}
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						if (StringUtils.isNotBlank(forecastActual.getForecastReason())) {
							obxSetId++;
							String loinc = "30982-3";
							String loincLabel = "Reason for recommendation";
							String value = forecastActual.getForecastReason();
							String valueLabel = "";
							String valueTable = "";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						if (forecastActual.getValidDate() != null) {
							obxSetId++;
							String loinc = VaccinationRecommendationDateCode.EARLIEST.getCode();
							String loincLabel = VaccinationRecommendationDateCode.EARLIEST.getLabel();
							Date value = forecastActual.getValidDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = VaccinationRecommendationDateCode.DUE.getCode();
							String loincLabel = VaccinationRecommendationDateCode.DUE.getLabel();
							Date value = forecastActual.getDueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getOverdueDate() != null) {
							obxSetId++;
							String loinc = VaccinationRecommendationDateCode.OVERDUE.getCode();
							String loincLabel = VaccinationRecommendationDateCode.OVERDUE.getLabel();
							Date value = forecastActual.getOverdueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getFinishedDate() != null) {
							obxSetId++;
							String loinc = VaccinationRecommendationDateCode.LATEST.getCode();
							String loincLabel = VaccinationRecommendationDateCode.LATEST.getLabel();
							Date value = forecastActual.getFinishedDate(); // TODO  make sure it is the right date
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}

//						if (StringUtils.isNotBlank(forecastActual.getScheduleName())) {
//							obxSetId++;
//							String loinc = "59779-9";
//							String loincLabel = "Schedule used";
//							String value = forecastActual.getScheduleName();
//							String valueLabel = "";
//							String valueTable = "";
//							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
//						}
						if (StringUtils.isNotBlank(forecastActual.getScheduleName())) {
							obxSetId++;
							String loinc = "59780-7";
							String loincLabel = "Series Name";
							String value = forecastActual.getScheduleName();
							String valueLabel = "";
							String valueTable = "";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
					}
				}
			}
		}

		String messageResponse = sb.toString();
		messageRecordingService.recordMessageReceived(messageReceived, patientMaster, messageResponse, "Query", categoryResponse, tenant);
		return messageResponse;
	}

	private void printRXR(IisVaccination vaccination, StringBuilder sb) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		sb.append("RXR");
		// RXR-1
		sb.append("|");
		sb.append(hl7MessageWriter.printCode(vaccination.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT", codeMap));
		// RXR-2
		sb.append("|");
		sb.append(hl7MessageWriter.printCode(vaccination.getBodySite(), CodesetType.BODY_SITE, "HL70163", codeMap));
		sb.append("\r");
	}

	private void printRXA(IisVaccination vaccination, StringBuilder sb, int obxSetId, Set<ProcessingFlavor> processingFlavorSet, Code cvxCode) {
		SimpleDateFormat sdf = v2DateParseService.generateSimpleDateFormat();
		CodeMap codeMap = codeMapManagerService.getCodeMap();

		sb.append("RXA");
		// RXA-1
		sb.append("|0");
		// RXA-2
		sb.append("|1");
		String adminDate = sdf.format(vaccination.getAdministeredDate());
		if (obxSetId == 0 && processingFlavorSet.contains(ProcessingFlavor.CHERRY)) {
			adminDate = "";
		}
		// RXA-3
		sb.append("|").append(adminDate);
		// RXA-4
		sb.append("|");
		// RXA-5
		sb.append("|").append(cvxCode.getValue()).append("^").append(cvxCode.getLabel()).append("^CVX");
		if (StringUtils.isNotBlank(vaccination.getVaccineNdcCode())) {
			Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccination.getVaccineNdcCode());
			if (ndcCode != null) {
				sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
			}
		}
		{
			// RXA-6
			sb.append("|");
			double adminAmount = 0.0;
			if (StringUtils.isNotBlank(vaccination.getAdministeredAmount())) {
				try {
					adminAmount = Double.parseDouble(vaccination.getAdministeredAmount());
				} catch (NumberFormatException nfe) {
					adminAmount = 0.0;
				}
			}
			if (adminAmount > 0) {
				if (adminAmount == 999.0) {
					sb.append("999");
				} else {
					sb.append(adminAmount);
				}
			}
			// RXA-7
			sb.append("|");
			if (adminAmount > 0) {
				sb.append("mL^milliliters^UCUM");
			}
		}
		// RXA-8
		sb.append("|");
		// RXA-9
		sb.append("|");
		{
			Code informationCode = null;
			if (vaccination.getInformationSource() != null) {
				informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE, vaccination.getInformationSource());
			}
			if (informationCode != null) {
				sb.append(informationCode.getValue()).append("^").append(informationCode.getLabel()).append("^NIP001");
			}
		}
		// RXA-10
		sb.append("|");
		// RXA-11
		sb.append("|");
		if (vaccination.getOrgLocation() == null || StringUtils.isBlank(vaccination.getOrgLocation().getOrgFacilityCode())) {
		} else {
			sb.append("^^^");
			sb.append(vaccination.getOrgLocation().getOrgFacilityCode());
		}
		// RXA-12
		sb.append("|");
		// RXA-13
		sb.append("|");
		// RXA-14
		sb.append("|");
		// RXA-15
		sb.append("|");
		if (vaccination.getLotnumber() != null) {
			sb.append(vaccination.getLotnumber());
		}
		// RXA-16
		sb.append("|");
		if (vaccination.getExpirationDate() != null) {
			sb.append(sdf.format(vaccination.getExpirationDate()));
		}
		// RXA-17
		sb.append("|");
		sb.append(hl7MessageWriter.printCode(vaccination.getVaccineMvxCode(), CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
		// RXA-18
		sb.append("|");
		sb.append(hl7MessageWriter.printCode(vaccination.getRefusalReasonCode(), CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
		// RXA-19
		sb.append("|");
		// RXA-20
		sb.append("|");
		if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
			String completionStatus = vaccination.getCompletionStatus();
			if (StringUtils.isBlank(completionStatus)) {
				completionStatus = "CP";
			}
			sb.append(hl7MessageWriter.printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
		}

		// RXA-21
		sb.append("|A");
		sb.append("\r");
	}

	public List<ForecastActual> doForecast(IisPatient patient, List<? extends IisVaccination> vaccinationMasterList, Tenant tenant, Date date) {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		List<ForecastActual> forecastActualList = null;
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			TestCase testCase = new TestCase();
			testCase.setEvalDate(date);
			if (patient != null) {
				testCase.setPatientSex(patient.getSex());
				testCase.setPatientDob(patient.getBirthDate());
			} else {
				testCase.setPatientSex("F");
			}
			List<TestEvent> testEventList = new ArrayList<>();
			for (IisVaccination vaccination : vaccinationMasterList) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					continue;
				}
				if ("D".equals(vaccination.getActionCode())) {
					continue;
				}
				int cvx;
				try {
					cvx = Integer.parseInt(vaccination.getVaccineCvxCode());
					TestEvent testEvent = new TestEvent(cvx, vaccination.getAdministeredDate());
					testEventList.add(testEvent);
					vaccination.setTestEvent(testEvent);
				} catch (NumberFormatException ignored) {
				}
			}
			testCase.setTestEventList(testEventList);
			Software software = new Software();
			software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/lonestar/forecast");
			software.setService(org.immregistries.vfa.connect.model.Service.LSVF);
			if (processingFlavorSet.contains(ProcessingFlavor.ICE)) {
				software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/opencds-decision-support-service/evaluate");
				software.setService(org.immregistries.vfa.connect.model.Service.ICE);
			}

			ConnectorInterface connector = ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
			connector.setLogText(false);
			try {

				SoftwareResult softwareResult = new SoftwareResult();
				forecastActualList = connector.queryForForecast(testCase, softwareResult);
//				logger.info("swr {}", softwareResult.getLogText());
			} catch (IOException ioe) {
				logger.error("Unable to query for forecast", ioe);
			}
		} catch (Exception e) {
			logger.error("Unable to query for forecast", e);
		}
		return forecastActualList;
	}

}
