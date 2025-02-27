package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.ack.IisHL7Util;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.ConnectFactory;
import org.immregistries.vfa.connect.ConnectorInterface;
import org.immregistries.vfa.connect.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.logic.IIncomingMessageHandler.*;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;

@org.springframework.stereotype.Service
public class IncomingQueryHandler {

	@Autowired
	AbstractFhirRequester fhirRequester;
	@Autowired
	AbstractHl7MessageWriter hl7MessageWriter;
	@Autowired
	ValidationService validationService;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;


	@Autowired
	MessageRecordingService messageRecordingService;

	@Autowired
	FhirContext fhirContext;

	public String processQBP(Tenant tenant, HL7Reader reader, String messageReceived) throws Exception {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = validationService.getMqeMessageService().processMessage(messageReceived);
		List<IisReportable> reportables = validationService.nistValidation(messageReceived, mqeMessageServiceResponse.getMessageObjects().getMessageHeader().getMessageProfile());
		PatientMaster patientMasterForMatchQuery = new PatientMaster();
		if (reader.advanceToSegment("QPD")) {
			String mrn = "";
			{
				mrn = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
				if (StringUtils.isBlank(mrn)) {
					mrn = reader.getValueBySearchingRepeats(3, 1, "PT", 5);
				}
			}
			String problem = null;
			int fieldPosition = 0;
			if (StringUtils.isNotBlank(mrn)) {
				BusinessIdentifier businessIdentifier = new BusinessIdentifier();
				businessIdentifier.setValue(mrn);
				patientMasterForMatchQuery.addBusinessIdentifier(businessIdentifier);// TODO system
//				patientMasterForMatchQuery.setExternalLink(mrn); // TODO system
//				patientReported = fhirRequester.searchPatientReported(
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

			Date patientBirthDate = IIncomingMessageHandler.parseDateWarn(reader.getValue(6), "Invalid patient birth date", "QPD", 1, 6, strictDate, reportables);
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
				reportables.add(IisReportable.fromProcessingException(new ProcessingException(problem, "QPD", 1, fieldPosition)));
			} else {
				ModelName modelName = new ModelName(patientNameLast, patientNameFirst, patientNameMiddle, "");
				patientMasterForMatchQuery.addPatientName(modelName);
				patientMasterForMatchQuery.setBirthDate(patientBirthDate);
			}
		} else {
			reportables.add(IisReportable.fromProcessingException(new ProcessingException("QPD segment not found", null, 0, 0)));
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
		PatientMaster singleMatch = fhirRequester.matchPatient(multipleMatches, patientMasterForMatchQuery, cutoff);
		if (singleMatch == null) {
			throw new ProcessingException("Patient not found", "PID", 1, 1); // TODO position
		}

		return buildRSP(reader, messageReceived, singleMatch, tenant, multipleMatches, reportables);
	}

	public String buildRSP(HL7Reader reader, String messageReceived, PatientMaster patientMaster, Tenant tenant, List<PatientReported> patientReportedPossibleList, List<IisReportable> iisReportables) {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = validationService.getMqeMessageService().processMessage(messageReceived);
		boolean sendInformations = true;
		if (processingFlavorSet.contains(ProcessingFlavor.STARFRUIT) && (StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("S") || StringUtils.defaultString(patientMaster.getNameFirst()).startsWith("A"))) {
			iisReportables.add(IisReportable.fromProcessingException(new ProcessingException("Immunization History cannot be shared because of patient's consent status", "PID", 0, 0, IisReportableSeverity.NOTICE)));
			sendInformations = false;
		}
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		reader.resetPostion();
		reader.advanceToSegment("MSH");

		StringBuilder sb = new StringBuilder();
		String profileIdSubmitted = reader.getValue(21);
		CodeMap codeMap = CodeMapManager.getCodeMap();
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
				iisReportables.add(IisReportable.fromProcessingException(new ProcessingException("Unrecognized profile id '" + profileIdSubmitted + "'", "MSH", 1, 21)));
			}
			// TODO remove notices ?
			hl7MessageWriter.createMSH(RSP_K_11_RSP_K_11, profileId, reader, sb, processingFlavorSet);
		}

		{
			String sendersUniqueId = reader.getValue(10);
			IisHL7Util.makeMsaAndErr(sb, sendersUniqueId, profileId, profileId, iisReportables, processingFlavorSet);
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
				SimpleDateFormat sdf = IIncomingMessageHandler.generateV2SDF();
				int count = 0;
				for (PatientReported pr : patientReportedPossibleList) {
					count++;
					PatientMaster patient = pr.getPatientMaster();
					hl7MessageWriter.printQueryPID(pr, processingFlavorSet, sb, patient, sdf, count);
				}
			} else if (profileId.equals(RSP_Z32_MATCH) || profileId.equals(RSP_Z42_MATCH_WITH_FORECAST)) {
				/**
				 * CONFUSING naming p but no better solution right now but to deal with single match
				 */
				PatientMaster patient = patientMaster;
				SimpleDateFormat sdf = IIncomingMessageHandler.generateV2SDF();
				hl7MessageWriter.printQueryPID(patientMaster, processingFlavorSet, sb, patient, sdf, 1);
				if (profileId.equals(RSP_Z32_MATCH)) {
					hl7MessageWriter.printQueryNK1(patientMaster, sb, codeMap);
				}
				List<VaccinationMaster> vaccinationMasterList = getVaccinationMasterList(patientMaster);

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
					forecastActualList = doForecast(patientMaster, codeMap, vaccinationMasterList, tenant);
				}
				int obxSetId = 0;
				int obsSubId = 0;
				for (VaccinationMaster vaccination : vaccinationMasterList) {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccination.getVaccineCvxCode());
					if (cvxCode == null) {
						continue;
					}
					boolean originalReporter = vaccination.getPatientReported().getTenant().equals(tenant);
					if ("D".equals(vaccination.getActionCode())) {
						continue;
					}
					hl7MessageWriter.printORC(tenant, sb, vaccination, originalReporter);
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
					if (vaccination.getOrgLocation() == null || vaccination.getOrgLocation().getOrgFacilityCode() == null || "".equals(vaccination.getOrgLocation().getOrgFacilityCode())) {
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
					if (StringUtils.isNotBlank(vaccination.getBodyRoute())) {
						sb.append("RXR");
						// RXR-1
						sb.append("|");
						sb.append(hl7MessageWriter.printCode(vaccination.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT", codeMap));
						// RXR-2
						sb.append("|");
						sb.append(hl7MessageWriter.printCode(vaccination.getBodySite(), CodesetType.BODY_SITE, "HL70163", codeMap));
						sb.append("\r");
					}
					TestEvent testEvent = vaccination.getTestEvent();
					if (testEvent != null && testEvent.getEvaluationActualList() != null) {
						for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
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
								String valueLabel = evaluationActual.getVaccineCvx();
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
						}
					}
					if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
						try {
							org.hl7.fhir.r5.model.Bundle bundle = fhirClient.search().forResource("Observation")
								.where(org.hl7.fhir.r5.model.Observation.PART_OF.hasId(patientMaster.getPatientId()))
								.and(org.hl7.fhir.r5.model.Observation.PART_OF.hasId(vaccination.getVaccinationId()))
								.returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
							if (bundle.hasEntry()) {
								obsSubId++;
								for (org.hl7.fhir.r5.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//						ObservationMaster observationMaster = ObservationMapper.getMaster((Observation) entry.getResource());
									ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
									obxSetId++;
									hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
								}
							}
						} catch (ResourceNotFoundException ignored) {
						}
					} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
						try {
							org.hl7.fhir.r4.model.Bundle bundle = fhirClient.search().forResource("Observation")
								.where(org.hl7.fhir.r4.model.Observation.PART_OF.hasId(patientMaster.getPatientId()))
								.and(org.hl7.fhir.r4.model.Observation.PART_OF.hasId(vaccination.getVaccinationId()))
								.returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
							if (bundle.hasEntry()) {
								obsSubId++;
								for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//						ObservationMaster observationMaster = ObservationMapper.getMaster((Observation) entry.getResource());
									ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
									obxSetId++;
									hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
								}
							}
						} catch (ResourceNotFoundException ignored) {
						}
					}
				}
				if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
					try {
						org.hl7.fhir.r5.model.Bundle bundle = fhirClient.search()
							.forResource(org.hl7.fhir.r5.model.Observation.class)
							.where(org.hl7.fhir.r5.model.Observation.PART_OF.hasId(patientMaster.getPatientId()))
							.returnBundle(org.hl7.fhir.r5.model.Bundle.class)
							.execute();
						if (bundle.hasEntry()) {
							hl7MessageWriter.printORC(tenant, sb, null, false);
							obsSubId++;
							for (org.hl7.fhir.r5.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
								obxSetId++;
								ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
							}
						}
					} catch (ResourceNotFoundException ignored) {
					}
				} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
					try {
						org.hl7.fhir.r4.model.Bundle bundle = fhirClient.search()
							.forResource(org.hl7.fhir.r4.model.Observation.class)
							.where(org.hl7.fhir.r4.model.Observation.PART_OF.hasId(patientMaster.getPatientId()))
							.returnBundle(org.hl7.fhir.r4.model.Bundle.class)
							.execute();
						if (bundle.hasEntry()) {
							hl7MessageWriter.printORC(tenant, sb, null, false);
							obsSubId++;
							for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
								obxSetId++;
								ObservationReported observationReported = observationMapper.localObjectReported(entry.getResource());
								hl7MessageWriter.printObx(sb, obxSetId, obsSubId, observationReported);
							}
						}
					} catch (ResourceNotFoundException ignored) {
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
					sb.append("|NA");
					sb.append("\r");
					HashSet<String> cvxAddedSet = new HashSet<String>();
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
							String loincLabel = "Status in series";
							Admin admin = forecastActual.getAdmin();
							String value = admin.getAdminStatus();
							String valueLabel = admin.getLabel();
							String valueTable = "99106";
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "30981-5";
							String loincLabel = "Earliest date";
							Date value = forecastActual.getValidDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "30980-7";
							String loincLabel = "Recommended date";
							Date value = forecastActual.getDueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
						if (forecastActual.getDueDate() != null) {
							obxSetId++;
							String loinc = "59778-1";
							String loincLabel = "Latest date";
							Date value = forecastActual.getOverdueDate();
							hl7MessageWriter.printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
						}
					}
				}
			}
		}

		String messageResponse = sb.toString();
		messageRecordingService.recordMessageReceived(messageReceived, patientMaster, messageResponse, "Query", categoryResponse, tenant);
		return messageResponse;
	}

	public List<ForecastActual> doForecast(PatientMaster patient, CodeMap codeMap, List<VaccinationMaster> vaccinationMasterList, Tenant tenant) {
		List<ForecastActual> forecastActualList = null;
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			TestCase testCase = new TestCase();
			testCase.setEvalDate(new Date());
			if (patient != null) {
				testCase.setPatientSex(patient.getSex());
				testCase.setPatientDob(patient.getBirthDate());
			} else {
				testCase.setPatientSex("F");
			}
			List<TestEvent> testEventList = new ArrayList<>();
			for (VaccinationMaster vaccination : vaccinationMasterList) {
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
			software.setService(Service.LSVF);
			if (processingFlavorSet.contains(ProcessingFlavor.ICE)) {
				software.setServiceUrl("https://sabbia.westus2.cloudapp.azure.com/opencds-decision-support-service/evaluate");
				software.setService(Service.ICE);
			}

			ConnectorInterface connector = ConnectFactory.createConnecter(software, VaccineGroup.getForecastItemList());
			connector.setLogText(false);
			try {
				forecastActualList = connector.queryForForecast(testCase, new SoftwareResult());
			} catch (IOException ioe) {
				System.err.println("Unable to query for forecast");
				ioe.printStackTrace();
			}
		} catch (Exception e) {
			System.err.println("Unable to query for forecast");
			e.printStackTrace(System.err);
		}
		return forecastActualList;
	}

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<VaccinationMaster> vaccinationMasterList = new ArrayList<>();
		Map<String, VaccinationMaster> map = new HashMap<>();
		IQuery<IBaseBundle> query = fhirClient.search().forResource("Immunization")
			.withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD)
			.sort()
			.ascending("identifier");
		if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
			try {
				org.hl7.fhir.r5.model.Bundle bundle = query
					.where(org.hl7.fhir.r5.model.Immunization.PATIENT.hasId(patient.getPatientId()))
					.returnBundle(org.hl7.fhir.r5.model.Bundle.class)
					.execute();
				for (org.hl7.fhir.r5.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					org.hl7.fhir.r5.model.Immunization immunization = (org.hl7.fhir.r5.model.Immunization) entry.getResource();
					if (immunization.getOccurrenceDateTimeType() != null) {
						String key = sdf.format(immunization.getOccurrenceDateTimeType().getValue());
						if (immunization.getVaccineCode() != null && StringUtils.isNotBlank(immunization.getVaccineCode().getText())) {
							key += key + immunization.getVaccineCode().getText();
							VaccinationMaster vaccinationMaster = immunizationMapper.localObject(immunization);
							map.put(key, vaccinationMaster);
						}
					}
				}
			} catch (ResourceNotFoundException ignored) {
			}
		} else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
			try {
				org.hl7.fhir.r4.model.Bundle bundle = query
					.where(org.hl7.fhir.r4.model.Immunization.PATIENT.hasId(patient.getPatientId()))
					.returnBundle(org.hl7.fhir.r4.model.Bundle.class)
					.execute();
				for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					org.hl7.fhir.r4.model.Immunization immunization = (org.hl7.fhir.r4.model.Immunization) entry.getResource();
					if (immunization.getOccurrenceDateTimeType() != null) {
						String key = sdf.format(immunization.getOccurrenceDateTimeType().getValue());
						if (immunization.getVaccineCode() != null && StringUtils.isNotBlank(immunization.getVaccineCode().getText())) {
							key += key + immunization.getVaccineCode().getText();
							VaccinationMaster vaccinationMaster = immunizationMapper.localObject(immunization);
							map.put(key, vaccinationMaster);
						}
					}
				}
			} catch (ResourceNotFoundException ignored) {
			}
		}
		List<String> keyList = new ArrayList<>(map.keySet());
		Collections.sort(keyList);
		for (String key : keyList) {
			vaccinationMasterList.add(map.get(key));
		}
		return vaccinationMasterList;
	}

}
