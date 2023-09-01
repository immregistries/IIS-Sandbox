package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.InternalClient.FhirRequester;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper.MRN_SYSTEM;

/**
 * DO NOT EDIT THE CONTENT OF THIS FILE
 * <p>
 * This is a literal copy of IncomingMessageHandlerR5 except for the name and imported FHIR Model package
 * Please paste any new content from R5 version here to preserve similarity in behavior
 */
@org.springframework.stereotype.Service()
@Conditional(OnR4Condition.class)
public class IncomingMessageHandlerR4 extends IncomingMessageHandler<Organization> {
	private final Logger logger = LoggerFactory.getLogger(IncomingMessageHandler.class);

	public String process(String message, OrgMaster orgMaster) {
		HL7Reader reader = new HL7Reader(message);
		String messageType = reader.getValue(9);
		String responseMessage;
		try {
			Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
			String facilityId = reader.getValue(4);

			if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
				if (!facilityId.equals(orgMaster.getOrganizationName())) {
					throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4",
						"MSH", 1, 4);
				}
			}
			Organization sendingOrganization = processSendingOrganization(reader);
			Organization managingOrganization = processManagingOrganization(reader);
			switch (messageType) {
				case "VXU":
					responseMessage = processVXU(orgMaster, reader, message, managingOrganization);
					break;
				case "ORU":
					responseMessage = processORU(orgMaster, reader, message, managingOrganization);
					break;
				case "QBP":
					responseMessage = processQBP(orgMaster, reader, message);
					break;
				default:
					ProcessingException pe = new ProcessingException("Unsupported message", "", 0, 0);
					List<ProcessingException> processingExceptionList = new ArrayList<>();
					processingExceptionList.add(pe);
					responseMessage = buildAck(reader, processingExceptionList);
					recordMessageReceived(message, null, responseMessage, "Unknown", "NAck",
						orgMaster);
					break;
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
			List<ProcessingException> processingExceptionList = new ArrayList<>();
			processingExceptionList.add(new ProcessingException(
				"Internal error prevented processing: " + e.getMessage(), null, 0, 0));
			responseMessage = buildAck(reader, processingExceptionList);
		}
		return responseMessage;
	}

	public String processQBP(OrgMaster orgMaster, HL7Reader reader, String messageReceived) {
		PatientReported patientReported = null;
		List<PatientReported> patientReportedPossibleList = new ArrayList<>(); // TODO fix this with mdm
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		if (reader.advanceToSegment("QPD")) {
			String mrn = "";
			{
				mrn = reader.getValueBySearchingRepeats(3, 1, "MR", 5);
				if (mrn.equals("")) {
					mrn = reader.getValueBySearchingRepeats(3, 1, "PT", 5);
				}
			}
			String problem = null;
			int fieldPosition = 0;
			if (!mrn.equals("")) {
				patientReported = fhirRequester.searchPatientReported(
					Patient.IDENTIFIER.exactly().systemAndCode(MRN_SYSTEM, mrn)
				);
			}
			String patientNameLast = reader.getValue(4, 1);
			String patientNameFirst = reader.getValue(4, 2);
			String patientNameMiddle = reader.getValue(4, 3);
			boolean strictDate = false;

			Date patientBirthDate = parseDateWarn(reader.getValue(6), "Invalid patient birth date", "QPD",
				1, 6, strictDate, processingExceptionList);
			String patientSex = reader.getValue(7);

			if (patientNameLast.equals("")) {
				problem = "Last name is missing";
				fieldPosition = 4;
			} else if (patientNameFirst.equals("")) {
				problem = "First name is missing";
				fieldPosition = 4;
			} else if (patientBirthDate == null) {
				problem = "Date of Birth is missing";
				fieldPosition = 6;
			}
			if (problem != null) {
				processingExceptionList.add(new ProcessingException(problem, "QPD", 1, fieldPosition));
			} else {

				if (patientReported == null) {
					patientReported = fhirRequester.searchPatientReported(
						Patient.NAME.matches().value(patientNameFirst),
						Patient.FAMILY.matches().value(patientNameLast),
						Patient.BIRTHDATE.exactly().day(patientBirthDate)
					);
				}
				if (patientReported != null) {
					int points = 0;
					if (!patientNameLast.isBlank() && patientNameLast.equalsIgnoreCase(patientReported.getNameLast())) {
						points = points + 2;
					}
					if (!patientNameFirst.isBlank() && patientNameFirst.equalsIgnoreCase(patientReported.getNameFirst())) {
						points = points + 2;
					}
					if (!patientNameMiddle.isBlank() && patientNameMiddle.equalsIgnoreCase(patientReported.getNameFirst())) {
						points = points + 2;
					}
					if (patientBirthDate != null && patientBirthDate.equals(patientReported.getBirthDate())) {
						points = points + 2;
					}
					if (!patientSex.equals("")
						&& patientSex.equalsIgnoreCase(patientReported.getSex())) {
						points = points + 2;
					}
					if (points < 6) {
						// not enough matching so don't indicate this as a match
						patientReported = null;
					}
				}
				if (patientReported == null) { // TODO change merging with MDM & FHIR ?
					patientReportedPossibleList = fhirRequester.searchPatientReportedList(
						Patient.NAME.matches().values(patientNameFirst, patientNameLast),
						Patient.BIRTHDATE.exactly().day(patientBirthDate));
				}
				if (patientReported != null
					&& patientNameMiddle.equalsIgnoreCase(PATIENT_MIDDLE_NAME_MULTI)) {
					patientReportedPossibleList.add(patientReported);
					patientReported = null;
				}
			}
		} else {
			processingExceptionList.add(new ProcessingException("QPD segment not found", null, 0, 0));
		}

		Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
		if (processingFlavorSet.contains(ProcessingFlavor.SNAIL)
			|| processingFlavorSet.contains(ProcessingFlavor.SNAIL30)
			|| processingFlavorSet.contains(ProcessingFlavor.SNAIL60)
			|| processingFlavorSet.contains(ProcessingFlavor.SNAIL90)) {
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
			Date cutoff = calendar.getTime();
			if (patientReported != null) {
				if (cutoff.before(patientReported.getReportedDate())) {
					patientReported = null;
				}
			}
			for (Iterator<PatientReported> it = patientReportedPossibleList.iterator(); it.hasNext(); ) {
				PatientReported pr = it.next();
				if (cutoff.before(pr.getReportedDate())) {
					it.remove();
				}
			}
		}

		return buildRSP(reader, messageReceived, patientReported, orgMaster,
			patientReportedPossibleList, processingExceptionList);
	}

	@SuppressWarnings("unchecked")
	public String processVXU(OrgMaster orgMaster, HL7Reader reader, String message, Organization managingOrganization) {
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		try {
			Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
			CodeMap codeMap = CodeMapManager.getCodeMap();

			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(orgMaster, reader, processingExceptionList,
				processingFlavorSet, codeMap, strictDate, null, managingOrganization);


			int orcCount = 0;
			int rxaCount = 0;
			int obxCount = 0;
			int vaccinationCount = 0;
			int refusalCount = 0;
			while (reader.advanceToSegment("ORC")) {
				orcCount++;
				VaccinationReported vaccinationReported = null;
//        VaccinationMaster vaccinationMaster = null;
				String vaccineCode = "";
				Date administrationDate = null;
				String vaccinationReportedExternalLink = reader.getValue(3);
				String vaccinationReportedExternalLinkSystem = reader.getValue(3, 2);
				if (reader.advanceToSegment("RXA", "ORC")) {
					rxaCount++;
					vaccineCode = reader.getValue(5, 1);
					if (vaccineCode.equals("")) {
						throw new ProcessingException("Vaccine code is not indicated in RXA-5.1", "RXA",
							rxaCount, 5);
					}
					if (vaccineCode.equals("998")) {
						obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported,
							strictDate, obxCount, null, null);
						continue;
					}
					if (vaccinationReportedExternalLink.equals("")) {
						throw new ProcessingException("Vaccination order id was not found, unable to process",
							"ORC", orcCount, 3);
					}
					administrationDate = parseDateError(reader.getValue(3, 1),
						"Could not read administered date in RXA-5", "RXA", rxaCount, 3, strictDate);
					if (administrationDate.after(new Date())) {
						throw new ProcessingException(
							"Vaccination is indicated as occuring in the future, unable to accept future vaccination events",
							"RXA", rxaCount, 3);
					}

					vaccinationReported = fhirRequester.searchVaccinationReported(Immunization.IDENTIFIER.exactly().code(vaccinationReportedExternalLink));
					if (vaccinationReported != null) {
//				 vaccinationMaster = vaccinationReported.getVaccination();
					}

					if (vaccinationReported == null) {
						vaccinationReported = new VaccinationReported();
						vaccinationReported.setReportedDate(new Date());
						vaccinationReported.setExternalLink(vaccinationReportedExternalLink);
						vaccinationReported.setExternalLinkSystem(vaccinationReportedExternalLinkSystem);
					}
					vaccinationReported.setPatientReportedId(patientReported.getPatientId());
					vaccinationReported.setPatientReported(patientReported);

					String vaccineCvxCode = "";
					String vaccineNdcCode = "";
					String vaccineCptCode = "";
					String vaccineCodeType = reader.getValue(5, 3);
					if (vaccineCodeType.equals("NDC")) {
						vaccineNdcCode = vaccineCode;
					} else if (vaccineCodeType.equals("CPT") || vaccineCodeType.equals("C4")
						|| vaccineCodeType.equals("C5")) {
						vaccineCptCode = vaccineCode;
					} else {
						vaccineCvxCode = vaccineCode;
					}
					{
						String altVaccineCode = reader.getValue(5, 4);
						String altVaccineCodeType = reader.getValue(5, 6);
						if (!altVaccineCode.equals("")) {
							if (altVaccineCodeType.equals("NDC")) {
								if (vaccineNdcCode.equals("")) {
									vaccineNdcCode = altVaccineCode;
								}
							} else if (altVaccineCodeType.equals("CPT") || altVaccineCodeType.equals("C4")
								|| altVaccineCodeType.equals("C5")) {
								if (vaccineCptCode.equals("")) {
									vaccineCptCode = altVaccineCode;
								}
							} else {
								if (vaccineCvxCode.equals("")) {
									vaccineCvxCode = altVaccineCode;
								}
							}
						}
					}

					{
						Code ndcCode =
							codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
						if (ndcCode != null) {
							if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null
								&& ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null
								&& !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
								vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
							}
							Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
							if (cvxCode == null) {
								ProcessingException pe =
									new ProcessingException("Unrecognized NDC " + vaccineNdcCode, "RXA", rxaCount,
										5).setWarning();
								processingExceptionList.add(pe);
							} else {
								if (vaccineCvxCode.equals("")) {
									vaccineCvxCode = cvxCode.getValue();
								} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
									// NDC doesn't map to the CVX code that was submitted!
									ProcessingException pe = new ProcessingException(
										"NDC " + vaccineNdcCode + " maps to " + cvxCode.getValue() + " but CVX "
											+ vaccineCvxCode + " was also reported, preferring CVX code",
										"RXA", rxaCount, 5);
									pe.setWarning();
									processingExceptionList.add(pe);
								}
							}
						}
					}
					{
						Code cptCode =
							codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCptCode);
						if (cptCode != null) {
							Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
							if (cvxCode == null) {
								ProcessingException pe =
									new ProcessingException("Unrecognized CPT " + cptCode, "RXA", rxaCount, 5)
										.setWarning();
								processingExceptionList.add(pe);
							} else {
								if (vaccineCvxCode.equals("")) {
									vaccineCvxCode = cvxCode.getValue();
								} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
									// CPT doesn't map to the CVX code that was submitted!
									ProcessingException pe = new ProcessingException(
										"CPT " + vaccineCptCode + " maps to " + cvxCode.getValue() + " but CVX "
											+ vaccineCvxCode + " was also reported, preferring CVX code",
										"RXA", rxaCount, 5).setWarning();
									processingExceptionList.add(pe);
								}
							}
						}
					}
					if (vaccineCvxCode.equals("")) {
						throw new ProcessingException(
							"Unable to find a recognized vaccine administration code (CVX, NDC, or CPT)", "RXA",
							rxaCount, 5);
					} else {
						Code cvxCode =
							codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCvxCode);
						if (cvxCode != null) {
							vaccineCvxCode = cvxCode.getValue();
						} else {
							throw new ProcessingException("Unrecognized CVX vaccine '" + vaccineCvxCode + "'",
								"RXA", rxaCount, 5);
						}

					}


					{
						String administeredAtLocation = reader.getValue(11, 4);
						if (StringUtils.isNotEmpty(administeredAtLocation)) {
							OrgLocation orgLocation = fhirRequester.searchOrgLocation(Location.IDENTIFIER.exactly().code(administeredAtLocation));

							if (orgLocation == null) {
								if (processingFlavorSet.contains(ProcessingFlavor.PEAR)) {
									throw new ProcessingException(
										"Unrecognized administered at location, unable to accept immunization report",
										"RXA", rxaCount, 11);
								}
								orgLocation = new OrgLocation();
								orgLocation.setOrgFacilityCode(administeredAtLocation);
								orgLocation.setOrgMaster(orgMaster);
								orgLocation.setOrgFacilityName(administeredAtLocation);
								orgLocation.setLocationType("");
								orgLocation.setAddressLine1(reader.getValue(11, 9));
								orgLocation.setAddressLine2(reader.getValue(11, 10));
								orgLocation.setAddressCity(reader.getValue(11, 11));
								orgLocation.setAddressState(reader.getValue(11, 12));
								orgLocation.setAddressZip(reader.getValue(11, 13));
								orgLocation.setAddressCountry(reader.getValue(11, 14));
								orgLocation = fhirRequester.saveOrgLocation(orgLocation);
							}
							vaccinationReported.setOrgLocation(orgLocation);
						}
					}
					{
						String administeringProvider = reader.getValue(10);
						if (StringUtils.isNotEmpty(administeringProvider)) {
							ModelPerson modelPerson = fhirRequester.searchPractitioner(Practitioner.IDENTIFIER.exactly().code(administeringProvider));
							if (modelPerson == null) {
								modelPerson = new ModelPerson();
								modelPerson.setPersonExternalLink(administeringProvider);
								modelPerson.setOrgMaster(orgMaster);
								modelPerson.setNameLast(reader.getValue(10, 2));
								modelPerson.setNameFirst(reader.getValue(10, 3));
								modelPerson.setNameMiddle(reader.getValue(10, 4));
								modelPerson.setAssigningAuthority(reader.getValue(10, 9));
								modelPerson.setNameTypeCode(reader.getValue(10, 10));
								modelPerson.setIdentifierTypeCode(reader.getValue(10, 13));
								modelPerson.setProfessionalSuffix(reader.getValue(10, 21));
//					  Person  p = PersonMapper.getFhirPerson(modelPerson);
								modelPerson = fhirRequester.savePractitioner(modelPerson);
							}
							vaccinationReported.setAdministeringProvider(modelPerson);
						}

					}
//          vaccinationMaster.setVaccineCvxCode(vaccineCvxCode);
//          vaccinationMaster.setAdministeredDate(administrationDate);
					vaccinationReported.setUpdatedDate(new Date());
					vaccinationReported.setAdministeredDate(administrationDate);
					vaccinationReported.setVaccineCvxCode(vaccineCvxCode);
					vaccinationReported.setVaccineNdcCode(vaccineNdcCode);
					vaccinationReported.setAdministeredAmount(reader.getValue(6));
					vaccinationReported.setInformationSource(reader.getValue(9));
					vaccinationReported.setLotnumber(reader.getValue(15));
					vaccinationReported.setExpirationDate(
						parseDateWarn(reader.getValue(16), "Invalid vaccination expiration date", "RXA",
							rxaCount, 16, strictDate, processingExceptionList));
					vaccinationReported.setVaccineMvxCode(reader.getValue(17));
					vaccinationReported.setRefusalReasonCode(reader.getValue(18));
					vaccinationReported.setCompletionStatus(reader.getValue(20));
					if (!vaccinationReported.getRefusalReasonCode().equals("")) {
						Code refusalCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_REFUSAL,
							vaccinationReported.getRefusalReasonCode());
						if (refusalCode == null) {
							ProcessingException pe =
								new ProcessingException("Unrecognized refusal reason", "RXA", rxaCount, 18);
							pe.setWarning();
							processingExceptionList.add(pe);
						}
					}
					vaccinationReported.setActionCode(reader.getValue(21));
					int segmentPosition = reader.getSegmentPosition();
					if (reader.advanceToSegment("RXR", "ORC")) {
						vaccinationReported.setBodyRoute(reader.getValue(1));
						vaccinationReported.setBodySite(reader.getValue(2));
					} else if (processingFlavorSet.contains(ProcessingFlavor.SPRUCE)) {
						if (vaccinationReported.getInformationSource().equals("00")) {
							throw new ProcessingException("RXR segment is required for administered vaccinations",
								"RXA", rxaCount, 0);
						}
					}
					if (vaccinationReported.getAdministeredDate()
						.before(patientReported.getBirthDate())
						&& !processingFlavorSet.contains(ProcessingFlavor.CLEMENTINE)) {
						throw new ProcessingException(
							"Vaccination is reported as having been administered before the patient was born",
							"RXA", rxaCount, 3);
					}
					if (!vaccinationReported.getVaccineCvxCode().equals("998")
						&& !vaccinationReported.getVaccineCvxCode().equals("999")
						&& (vaccinationReported.getCompletionStatus().equals("CP")
						|| vaccinationReported.getCompletionStatus().equals("PA")
						|| vaccinationReported.getCompletionStatus().equals(""))) {
						vaccinationCount++;
					}

					if (vaccinationReported.getCompletionStatus().equals("RE")) {
						refusalCount++;
					}


					reader.gotoSegmentPosition(segmentPosition);
					int tempObxCount = obxCount;
					while (reader.advanceToSegment("OBX", "ORC")) { //TODO store entering and ordering practitioners
						tempObxCount++;
						String indicator = reader.getValue(3);
						if (indicator.equals("64994-7")) {
							String fundingEligibility = reader.getValue(5);
							if (!fundingEligibility.equals("")) {
								Code fundingEligibilityCode = codeMap
									.getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, fundingEligibility);
								if (fundingEligibilityCode == null) {
									ProcessingException pe = new ProcessingException(
										"Funding eligibility '" + fundingEligibility + "' was not recognized", "OBX",
										tempObxCount, 5).setWarning();
									processingExceptionList.add(pe);
								} else {
									vaccinationReported.setFundingEligibility(fundingEligibilityCode.getValue());
								}
							}
						} else if (indicator.equals("30963-3")) {
							String fundingSource = reader.getValue(5);
							if (!fundingSource.equals("")) {
								Code fundingSourceCode = codeMap
									.getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, fundingSource);
								if (fundingSourceCode == null) {
									ProcessingException pe = new ProcessingException(
										"Funding source '" + fundingSource + "' was not recognized", "OBX",
										tempObxCount, 5).setWarning();
									processingExceptionList.add(pe);
								} else {
									vaccinationReported.setFundingSource(fundingSourceCode.getValue());
								}
							}
						}
					}

					verifyNoErrors(processingExceptionList);
					reader.gotoSegmentPosition(segmentPosition);
					vaccinationReported = fhirRequester.saveVaccinationReported(vaccinationReported);
					reader.gotoSegmentPosition(segmentPosition);
					obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported,
						strictDate, obxCount, vaccinationReported, null);
				} else {
					throw new ProcessingException("RXA segment was not found after ORC segment", "ORC",
						orcCount, 0);
				}
			}
			if (processingFlavorSet.contains(ProcessingFlavor.CRANBERRY) && vaccinationCount == 0) {
				throw new ProcessingException(
					"Patient vaccination history cannot be accepted without at least one administered or historical vaccination specified",
					"", 0, 0);
			}
			if (processingFlavorSet.contains(ProcessingFlavor.BILBERRY)
				&& (vaccinationCount == 0 && refusalCount == 0)) {
				throw new ProcessingException(
					"Patient vaccination history cannot be accepted without at least one administered, historical, or refused vaccination specified",
					"", 0, 0);
			}
			String ack = buildAck(reader, processingExceptionList);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", orgMaster);
			return ack;
		} catch (ProcessingException e) {
			if (!processingExceptionList.contains(e)) {
				processingExceptionList.add(e);
			}
			String ack = buildAck(reader, processingExceptionList);
			recordMessageReceived(message, null, ack, "Update", "Exception", orgMaster);
			return ack;
		}

	}

	@SuppressWarnings("unchecked")
	public PatientReported processPatient(OrgMaster orgMaster, HL7Reader reader,
													  List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet,
													  CodeMap codeMap, boolean strictDate, PatientReported patientReported, Organization managingOrganization)
		throws ProcessingException {

//    PatientMaster patientMaster = null;
		String patientReportedExternalLink = "";
		String patientReportedAuthority = "";
		String patientReportedType = "MR";
		if (reader.advanceToSegment("PID")) {
			patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
			patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
			if (patientReportedExternalLink.equals("")) {
				patientReportedAuthority = "";
				patientReportedType = "PT";
				patientReportedExternalLink =
					reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
				patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
				if (patientReportedExternalLink.equals("")) {
					patientReportedAuthority = "";
					patientReportedType = "PI";
					patientReportedExternalLink =
						reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
					patientReportedAuthority =
						reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
					if (patientReportedExternalLink.equals("")) {
						throw new ProcessingException(
							"MRN was not found, required for accepting vaccination report", "PID", 1, 3);
					}
				}
			}
		} else {
			throw new ProcessingException(
				"No PID segment found, required for accepting vaccination report", "", 0, 0);
		}
		patientReported = fhirRequester.searchPatientReported(
			Patient.IDENTIFIER.exactly().identifier(patientReportedExternalLink)
		);

		if (patientReported == null) {
//      patientMaster = new PatientMaster();
			patientReported = new PatientReported();
			patientReported.setOrgReported(orgMaster);
			patientReported.setExternalLink(patientReportedExternalLink);
			patientReported.setReportedDate(new Date());
			patientReported.setManagingOrganizationId(managingOrganization.getId());
		}

		{
			String patientNameLast = reader.getValue(5, 1);
			String patientNameFirst = reader.getValue(5, 2);
			String patientNameMiddle = reader.getValue(5, 3);
			String patientPhone = reader.getValue(13, 6) + reader.getValue(13, 7);
			String telUseCode = reader.getValue(13, 2);
			if (patientPhone.length() > 0) {
				if (!telUseCode.equals("PRN")) {
					ProcessingException pe = new ProcessingException(
						"Patient phone telecommunication type must be PRN ", "PID", 1, 13);
					if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}

				{
					int countNums = 0;
					boolean invalidCharFound = false;
					char invalidChar = ' ';
					for (char c : patientPhone.toCharArray()) {

						if (c >= '0' && c <= '9') {
							countNums++;
						} else if (c != '-' && c != '.' && c != ' ' && c != '(' && c != ')') {
							if (!invalidCharFound) {
								invalidCharFound = true;
								invalidChar = c;
							}
						}
					}
					if (invalidCharFound) {
						ProcessingException pe = new ProcessingException(
							"Patient phone number has unexpected character: " + invalidChar, "PID", 1, 13);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
					if (countNums != 10 || patientPhone.startsWith("555") || patientPhone.startsWith("0")
						|| patientPhone.startsWith("1")) {
						ProcessingException pe = new ProcessingException(
							"Patient phone number does not appear to be valid", "PID", 1, 13);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
				}
			}
			if (!telUseCode.equals("PRN")) {
				patientPhone = "";
			}

			if (patientNameLast.equals("")) {
				throw new ProcessingException(
					"Patient last name was not found, required for accepting patient and vaccination history",
					"PID", 1, 5);
			}
			if (patientNameFirst.equals("")) {
				throw new ProcessingException(
					"Patient first name was not found, required for accepting patient and vaccination history",
					"PID", 1, 5);
			}


			String zip = reader.getValue(11, 5);
			if (zip.length() > 5) {
				zip = zip.substring(0, 5);
			}
			String addressFragPrep = reader.getValue(11, 1);
			String addressFrag = "";
			{
				int spaceIndex = addressFragPrep.indexOf(" ");
				if (spaceIndex > 0) {
					addressFragPrep = addressFragPrep.substring(0, spaceIndex);
				}
				addressFrag = zip + ":" + addressFragPrep;
			}
			Date patientBirthDate;
			patientBirthDate = parseDateError(reader.getValue(7), "Bad format for date of birth", "PID",
				1, 7, strictDate);
			if (patientBirthDate.after(new Date())) {
				throw new ProcessingException(
					"Patient is indicated as being born in the future, unable to record patients who are not yet born",
					"PID", 1, 7);
			}
			patientReported.setExternalLink(patientReportedExternalLink);
			patientReported.setPatientReportedType(patientReportedType);
			patientReported.setNameFirst(patientNameFirst);
			patientReported.setNameLast(patientNameLast);
			patientReported.setNameMiddle(patientNameMiddle);
			patientReported.setMotherMaidenName(reader.getValue(6));
			patientReported.setBirthDate(patientBirthDate);
			patientReported.setSex(reader.getValue(8));
			patientReported.setRace(reader.getValue(10));
			patientReported.setRace2(reader.getValueRepeat(10, 1, 2));
			patientReported.setRace3(reader.getValueRepeat(10, 1, 3));
			patientReported.setRace4(reader.getValueRepeat(10, 1, 4));
			patientReported.setRace5(reader.getValueRepeat(10, 1, 5));
			patientReported.setRace6(reader.getValueRepeat(10, 1, 6));
			patientReported.setAddressLine1(reader.getValue(11, 1));
			patientReported.setAddressLine2(reader.getValue(11, 2));
			patientReported.setAddressCity(reader.getValue(11, 3));
			patientReported.setAddressState(reader.getValue(11, 4));
			patientReported.setAddressZip(reader.getValue(11, 5));
			patientReported.setAddressCountry(reader.getValue(11, 6));
			patientReported.setAddressCountyParish(reader.getValue(11, 9));
			patientReported.setEthnicity(reader.getValue(22));
			patientReported.setBirthFlag(reader.getValue(24));
			patientReported.setBirthOrder(reader.getValue(25));
			patientReported.setDeathDate(parseDateWarn(reader.getValue(29),
				"Invalid patient death date", "PID", 1, 29, strictDate, processingExceptionList));
			patientReported.setDeathFlag(reader.getValue(30));
			patientReported.setEmail(reader.getValueBySearchingRepeats(13, 4, "NET", 2));
			patientReported.setPhone(patientPhone);
			patientReported.setPatientReportedAuthority(patientReportedAuthority);

			{
				String patientSex = patientReported.getSex();
				if (!ValidValues.verifyValidValue(patientSex, ValidValues.SEX)) {
					ProcessingException pe =
						new ProcessingException("Patient sex '" + patientSex + "' is not recognized", "PID",
							1, 8).setWarning();
					if (processingFlavorSet.contains(ProcessingFlavor.ELDERBERRIES)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}

			String patientAddressCountry = patientReported.getAddressCountry();
			if (!patientAddressCountry.equals("")) {
				if (!ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_2DIGIT)
					&& !ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_3DIGIT)) {
					ProcessingException pe = new ProcessingException("Patient address country '"
						+ patientAddressCountry + "' is not recognized and cannot be accepted", "PID", 1, 11);
					if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
			if (patientAddressCountry.equals("") || patientAddressCountry.equals("US")
				|| patientAddressCountry.equals("USA")) {
				String patientAddressState = patientReported.getAddressState();
				if (!patientAddressState.equals("")) {
					if (!ValidValues.verifyValidValue(patientAddressState, ValidValues.STATE)) {
						ProcessingException pe = new ProcessingException("Patient address state '"
							+ patientAddressState + "' is not recognized and cannot be accepted", "PID", 1, 11);
						if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				}
			}


			{
				String race = patientReported.getRace();
				if (!race.equals("")) {
					Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
					if (raceCode == null
						|| CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
						ProcessingException pe = new ProcessingException(
							"Invalid race '" + race + "', message cannot be accepted", "PID", 1, 10);
						if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				}
			}

			{
				String ethnicity = patientReported.getEthnicity();
				if (!ethnicity.equals("")) {
					Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
					if (ethnicityCode == null
						|| CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
						ProcessingException pe = new ProcessingException(
							"Invalid ethnicity '" + ethnicity + "', message cannot be accepted", "PID", 1, 10);
						if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				}
			}

			if (processingFlavorSet.contains(ProcessingFlavor.BLACKBERRY)) {
				if (patientReported.getAddressLine1().equals("")
					|| patientReported.getAddressCity().equals("")
					|| patientReported.getAddressState().equals("")
					|| patientReported.getAddressZip().equals("")) {
					throw new ProcessingException("Patient address is required but it was not sent", "PID", 1,
						11);
				}
			}

			{
				String birthFlag = patientReported.getBirthFlag();
				String birthOrder = patientReported.getBirthOrder();
				if (!birthFlag.equals("") || !birthOrder.equals("")) {
					if (birthFlag.equals("") || birthFlag.equals("N")) {
						// The only acceptable value here is now blank or 1
						if (!birthOrder.equals("1") && !birthOrder.equals("")) {
							ProcessingException pe = new ProcessingException("Birth order was specified as "
								+ birthOrder + " but not indicated as multiple birth", "PID", 1, 25);
							if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
								pe.setWarning();
							}
							processingExceptionList.add(pe);
						}
					} else if (birthFlag.equals("Y")) {
						if (birthOrder.equals("")) {
							ProcessingException pe = new ProcessingException(
								"Multiple birth but birth order was not specified", "PID", 1, 24);
							pe.setWarning();
							processingExceptionList.add(pe);
						} else if (!ValidValues.verifyValidValue(birthOrder, ValidValues.BIRTH_ORDER)) {
							ProcessingException pe =
								new ProcessingException("Birth order was specified as " + birthOrder
									+ " but not an expected value, must be between 1 and 9", "PID", 1, 25);
							if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
								pe.setWarning();
							}
							processingExceptionList.add(pe);
						}
					} else {
						ProcessingException pe = new ProcessingException(
							"Multiple birth indicator " + birthFlag + " is not recognized", "PID", 1, 24);
						if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				}
			}

		}
		if (reader.advanceToSegment("PD1")) {
			patientReported.setPublicityIndicator(reader.getValue(11));
			patientReported.setProtectionIndicator(reader.getValue(12));
			patientReported.setProtectionIndicatorDate(parseDateWarn(reader.getValue(13),
				"Invalid protection indicator date", "PD1", 1, 13, strictDate, processingExceptionList));
			patientReported.setRegistryStatusIndicator(reader.getValue(16));
			patientReported.setRegistryStatusIndicatorDate(
				parseDateWarn(reader.getValue(17), "Invalid registry status indicator date", "PD1", 1, 17,
					strictDate, processingExceptionList));
			patientReported.setPublicityIndicatorDate(parseDateWarn(reader.getValue(18),
				"Invalid publicity indicator date", "PD1", 1, 18, strictDate, processingExceptionList));
		}
		reader.resetPostion();
		{
			int repeatCount = 0;
			while (reader.advanceToSegment("NK1")) {
				patientReported.setGuardianLast(reader.getValue(2, 1));
				patientReported.setGuardianFirst(reader.getValue(2, 2));
				patientReported.setGuardianMiddle(reader.getValue(2, 1));
				String guardianRelationship = reader.getValue(3);
				patientReported.setGuardianRelationship(guardianRelationship);
				repeatCount++;
				if (patientReported.getGuardianLast().equals("")) {
					ProcessingException pe =
						new ProcessingException("Next-of-kin last name is empty", "NK1", repeatCount, 2)
							.setWarning();
					processingExceptionList.add(pe);
				}
				if (patientReported.getGuardianFirst().equals("")) {
					ProcessingException pe =
						new ProcessingException("Next-of-kin first name is empty", "NK1", repeatCount, 2)
							.setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("")) {
					ProcessingException pe =
						new ProcessingException("Next-of-kin relationship is empty", "NK1", repeatCount, 3)
							.setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("MTH") || guardianRelationship.equals("FTH")
					|| guardianRelationship.equals("GRD")) {
					break;
				} else {
					ProcessingException pe = new ProcessingException((guardianRelationship.equals("")
						? "Next-of-kin relationship not specified so is not recognized as guardian and will be ignored"
						: ("Next-of-kin relationship '" + guardianRelationship
						+ "' is not a recognized guardian and will be ignored")),
						"NK1", repeatCount, 3).setWarning();
					processingExceptionList.add(pe);
				}
			}
		}
		reader.resetPostion();

		verifyNoErrors(processingExceptionList);

		patientReported.setUpdatedDate(new Date());
		patientReported = fhirRequester.savePatientReported(patientReported);
		patientReported = fhirRequester.saveRelatedPerson(patientReported);

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		ArrayList<String> groupPatientIds = (ArrayList<String>) request.getAttribute("groupPatientIds");
		if (groupPatientIds != null) { // If there are numerous patients added and option was activated
			groupPatientIds.add(patientReported.getPatientId());
		}
		request.setAttribute("groupPatientIds", groupPatientIds);


		return patientReported;
	}

	public String processORU(OrgMaster orgMaster, HL7Reader reader, String message, Organization managingOrganization) {
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		try {
			Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();

			CodeMap codeMap = CodeMapManager.getCodeMap();

			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(orgMaster, reader, processingExceptionList,
				processingFlavorSet, codeMap, strictDate, null, managingOrganization);

			int orcCount = 0;
			int obxCount = 0;
			while (reader.advanceToSegment("ORC")) {
				orcCount++;
				if (reader.advanceToSegment("OBR", "ORC")) {
					obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported,
						strictDate, obxCount, null, null);
				} else {
					throw new ProcessingException("OBR segment was not found after ORC segment", "ORC",
						orcCount, 0);
				}
			}
			String ack = buildAck(reader, processingExceptionList);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", orgMaster);
			return ack;
		} catch (ProcessingException e) {
			if (!processingExceptionList.contains(e)) {
				processingExceptionList.add(e);
			}
			String ack = buildAck(reader, processingExceptionList);
			recordMessageReceived(message, null, ack, "Update", "Exception", orgMaster);
			return ack;
		}
	}

	public int readAndCreateObservations(HL7Reader reader,
													 List<ProcessingException> processingExceptionList, PatientReported patientReported,
													 boolean strictDate, int obxCount, VaccinationReported vaccinationReported,
													 VaccinationMaster vaccination) {
		while (reader.advanceToSegment("OBX", "ORC")) {
			obxCount++;
			String identifierCode = reader.getValue(3);
			String valueCode = reader.getValue(5);
			ObservationReported observationReported =
				readObservations(reader, processingExceptionList, patientReported, strictDate, obxCount,
					vaccinationReported, vaccination, identifierCode, valueCode);
			if (observationReported.getIdentifierCode().equals("30945-0")) // contraindication!
			{
				CodeMap codeMap = CodeMapManager.getCodeMap();
				Code contraCode = codeMap.getCodeForCodeset(CodesetType.CONTRAINDICATION_OR_PRECAUTION,
					observationReported.getValueCode());
				if (contraCode == null) {
					ProcessingException pe = new ProcessingException(
						"Unrecognized contraindication or precaution", "OBX", obxCount, 5);
					pe.setWarning();
					processingExceptionList.add(pe);
				}
				if (observationReported.getObservationDate() != null) {
					Date today = new Date();
					if (observationReported.getObservationDate().after(today)) {
						ProcessingException pe = new ProcessingException(
							"Contraindication or precaution observed in the future", "OBX", obxCount, 5);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
					if (patientReported.getBirthDate() != null && observationReported
						.getObservationDate().before(patientReported.getBirthDate())) {
						ProcessingException pe = new ProcessingException(
							"Contraindication or precaution observed before patient was born", "OBX", obxCount,
							14);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
				}
			}
			{
				observationReported.setPatientReportedId(patientReported.getPatientId());

//		  Observation observation = ObservationMapper.getFhirResource(observationMaster,observationReported);
				observationReported = fhirRequester.saveObservationReported(observationReported);

			}
		}
		return obxCount;
	}

	@SuppressWarnings("unchecked")
	public ObservationReported readObservations(HL7Reader reader,
															  List<ProcessingException> processingExceptionList, PatientReported patientReported,
															  boolean strictDate, int obxCount, VaccinationReported vaccinationReported,
															  VaccinationMaster vaccination, String identifierCode, String valueCode) {
//    ObservationMaster observationMaster = null;
		ObservationReported observationReported = null;
		if (vaccination == null) {
			observationReported = fhirRequester.searchObservationReported(
				Observation.PART_OF.isMissing(true),
				Observation.SUBJECT.hasId(patientReported.getPatientId()));
		} else {
			observationReported = fhirRequester.searchObservationReported(
				Observation.PART_OF.hasId(vaccination.getVaccinationId()),
				Observation.SUBJECT.hasId(patientReported.getPatientId()));
		}
		if (observationReported == null) {
//      observationMaster = new ObservationMaster();
//      observationMaster.setPatientId(patientReported.getPatient().getPatientId());
//      observationMaster.setVaccination(vaccination);
//      observationMaster.setIdentifierCode(identifierCode);
			observationReported = new ObservationReported();
//      observationMaster.setObservationReported(observationReported);
			observationReported.setReportedDate(new Date());
		}
//    observationMaster.setValueCode(valueCode);

		observationReported.setPatientReportedId(patientReported.getPatientId());
		if (vaccinationReported != null) {
			observationReported.setVaccinationReportedId(vaccinationReported.getVaccinationId());
		}
//    observationReported.setObservation(observationMaster);
		observationReported.setUpdatedDate(new Date());
		observationReported.setIdentifierCode(identifierCode);
		observationReported.setValueType(reader.getValue(2));
		observationReported.setIdentifierLabel(reader.getValue(3, 2));
		observationReported.setIdentifierTable(reader.getValue(3, 3));
		observationReported.setValueCode(valueCode);
		observationReported.setValueLabel(reader.getValue(5, 2));
		observationReported.setValueTable(reader.getValue(5, 3));
		observationReported.setUnitsCode(reader.getValue(6, 1));
		observationReported.setUnitsLabel(reader.getValue(6, 2));
		observationReported.setUnitsTable(reader.getValue(6, 3));
		observationReported.setResultStatus(reader.getValue(11));
		observationReported.setObservationDate(
			parseDateWarn(reader.getValue(14), "Unparsable date/time of observation", "OBX", obxCount,
				14, strictDate, processingExceptionList));
		observationReported.setMethodCode(reader.getValue(17, 1));
		observationReported.setMethodLabel(reader.getValue(17, 2));
		observationReported.setMethodTable(reader.getValue(17, 3));
		return observationReported;
	}

	@SuppressWarnings("unchecked")
	public String buildRSP(HL7Reader reader, String messageReceived, PatientReported patientReported,
								  OrgMaster orgMaster, List<PatientReported> patientReportedPossibleList,
								  List<ProcessingException> processingExceptionList) { // TODO fix with mdm
		IGenericClient fhirClient = getFhirClient();
		reader.resetPostion();
		reader.advanceToSegment("MSH");

		Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
		StringBuilder sb = new StringBuilder();
		String profileIdSubmitted = reader.getValue(21);
		CodeMap codeMap = CodeMapManager.getCodeMap();
		String categoryResponse = "No Match";
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
			String messageType = "RSP^K11^RSP_K11";
			if (patientReported == null) {
				queryResponse = QUERY_NOT_FOUND;
				profileId = RSP_Z33_NO_MATCH;
				categoryResponse = "No Match";
				if (patientReportedPossibleList.size() > 0) {
					if (profileIdSubmitted.equals(QBP_Z34)) {
						if (patientReportedPossibleList.size() > maxCount) {
							queryResponse = QUERY_TOO_MANY;
							profileId = RSP_Z33_NO_MATCH;
							categoryResponse = "Too Many Matches";
						} else {
							queryResponse = QUERY_OK;
							profileId = RSP_Z31_MULTIPLE_MATCH;
							categoryResponse = "Possible Match";
						}
					} else if (profileIdSubmitted.equals("Z44")) {
						queryResponse = QUERY_NOT_FOUND;
						profileId = RSP_Z33_NO_MATCH;
						categoryResponse = "No Match";
					}
				}
				if (hasErrors(processingExceptionList)) {
					queryResponse = QUERY_APPLICATION_ERROR;
				}
			} else if (profileIdSubmitted.equals(QBP_Z34)) {
				profileId = RSP_Z32_MATCH;
				categoryResponse = "Match";
			} else if (profileIdSubmitted.equals(QBP_Z44)) {
				if (processingFlavorSet.contains(ProcessingFlavor.ORANGE)) {
					profileId = RSP_Z32_MATCH;
					categoryResponse = "Match";
				} else {
					sendBackForecast = true;
					profileId = RSP_Z42_MATCH_WITH_FORECAST;
					categoryResponse = "Match";
				}
			} else {
				processingExceptionList.add(new ProcessingException(
					"Unrecognized profile id '" + profileIdSubmitted + "'", "MSH", 1, 21));
			}
			createMSH(messageType, profileId, reader, sb, processingFlavorSet);
		}
		{
			String sendersUniqueId = reader.getValue(10);
			if (hasErrors(processingExceptionList)) {
				sb.append("MSA|AE|").append(sendersUniqueId).append("\r");
			} else {
				sb.append("MSA|AA|").append(sendersUniqueId).append("\r");
			}
			if (processingExceptionList.size() > 0) {
				printERRSegment(processingExceptionList.get(processingExceptionList.size() - 1), sb);
			}
		}
		String profileName = "Request a Complete Immunization History";
		if (profileIdSubmitted.equals("")) {
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
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			int count = 0;
			for (PatientReported pr : patientReportedPossibleList) {
				count++;
				PatientMaster patient = pr.getPatient();
				printQueryPID(pr, processingFlavorSet, sb, patient, sdf, count);
			}
		} else if (profileId.equals(RSP_Z32_MATCH) || profileId.equals(RSP_Z42_MATCH_WITH_FORECAST)) {
			PatientMaster patientMaster = patientReported.getPatient();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			printQueryPID(patientReported, processingFlavorSet, sb, patientMaster, sdf, 1);
			if (profileId.equals(RSP_Z32_MATCH)) {
				printQueryNK1(patientReported, sb, codeMap);
			}
			List<VaccinationMaster> vaccinationMasterList =
				getVaccinationMasterList(patientMaster);

			if (processingFlavorSet.contains(ProcessingFlavor.LEMON)) {
				for (Iterator<VaccinationMaster> it = vaccinationMasterList.iterator(); it.hasNext(); ) {
					it.next();
					if (random.nextInt(4) == 0) {
						it.remove();
					}
				}
			}
			if (processingFlavorSet.contains(ProcessingFlavor.GREEN)) {
				vaccinationMasterList.removeIf(vaccinationMaster -> vaccinationMaster.getVaccineCvxCode().equals("91"));
			}
			List<ForecastActual> forecastActualList = null;
			if (sendBackForecast) {
				forecastActualList =
					doForecast(patientMaster, patientReported, codeMap, vaccinationMasterList, orgMaster);
			}
			int obxSetId = 0;
			int obsSubId = 0;
			for (VaccinationMaster vaccination : vaccinationMasterList) {
				Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
					vaccination.getVaccineCvxCode());
				if (cvxCode == null) {
					continue;
				}
				boolean originalReporter =
					vaccination.getPatientReported().getOrgReported().equals(orgMaster);
				if ("D".equals(vaccination.getActionCode())) {
					continue;
				}
				printORC(orgMaster, sb, vaccination, originalReporter);
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
				if (!vaccination.getVaccineNdcCode().equals("")) {
					Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE,
						vaccination.getVaccineNdcCode());
					if (ndcCode != null) {
						sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
					}
				}
				{
					// RXA-6
					sb.append("|");
					double adminAmount = 0.0;
					if (!vaccination.getAdministeredAmount().equals("")) {
						try {
							adminAmount = Double.parseDouble(vaccination.getAdministeredAmount());
						} catch (NumberFormatException nfe) {
							adminAmount = 0.0;
						}
					}
					if (adminAmount > 0) {
						sb.append(adminAmount);
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
						informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccination.getInformationSource());
					}
					if (informationCode != null) {
						sb.append(informationCode.getValue()).append("^").append(informationCode.getLabel()).append("^NIP001");
					}
				}
				// RXA-10
				sb.append("|");
				// RXA-11
				sb.append("|");
				if (vaccination.getOrgLocation() == null
					|| vaccination.getOrgLocation().getOrgFacilityCode() == null
					|| "".equals(vaccination.getOrgLocation().getOrgFacilityCode())) {
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
				sb.append(printCode(vaccination.getVaccineMvxCode(),
					CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
				// RXA-18
				sb.append("|");
				sb.append(printCode(vaccination.getRefusalReasonCode(),
					CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
				// RXA-19
				sb.append("|");
				// RXA-20
				sb.append("|");
				if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
					String completionStatus = vaccination.getCompletionStatus();
					if (completionStatus == null || completionStatus.equals("")) {
						completionStatus = "CP";
					}
					sb.append(printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
				}

				// RXA-21
				sb.append("|A");
				sb.append("\r");
				if (vaccination.getBodyRoute() != null
					&& !vaccination.getBodyRoute().equals("")) {
					sb.append("RXR");
					// RXR-1
					sb.append("|");
					sb.append(printCode(vaccination.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT",
						codeMap));
					// RXR-2
					sb.append("|");
					sb.append(printCode(vaccination.getBodySite(), CodesetType.BODY_SITE, "HL70163",
						codeMap));
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
							String valueLabel = evaluationActual.getVaccineCvx();
							String valueTable = "CVX";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59781-5";
							String loincLabel = "Dose validity";
							String value = evaluationActual.getDoseValid();
							String valueLabel = value;
							String valueTable = "99107";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
					}
				}
				try {
					Bundle bundle = fhirClient.search().forResource(Observation.class)
						.where(Observation.PART_OF.hasId(patientMaster.getPatientId()))
						.and(Observation.PART_OF.hasId(vaccination.getVaccinationId()))
						.returnBundle(Bundle.class).execute();
					if (bundle.hasEntry()) {
						obsSubId++;
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
//						ObservationMaster observationMaster =
//							ObservationMapper.getMaster((Observation) entry.getResource());
							ObservationReported observationReported =
								observationMapper.getReported((Observation) entry.getResource());
							obxSetId++;
							printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException e) {
				}
			}
			try {
				Bundle bundle = fhirClient.search().forResource(Observation.class)
					.where(Observation.PART_OF.hasId(patientMaster.getPatientId()))
					.returnBundle(Bundle.class).execute();
				if (bundle.hasEntry()) {
					printORC(orgMaster, sb, null, false);
					obsSubId++;
					for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
						obxSetId++;
						ObservationReported observationReported =
							observationMapper.getReported((Observation) entry.getResource());
						printObx(sb, obxSetId, obsSubId, observationReported);
					}
				}
			} catch (ResourceNotFoundException e) {
			}

			if (sendBackForecast && forecastActualList != null && forecastActualList.size() > 0) {
				printORC(orgMaster, sb, null, false);
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
				for (ForecastActual forecastActual : forecastActualList) {
					obsSubId++;
					{
						obxSetId++;
						String loinc = "30956-7";
						String loincLabel = "Vaccine type";
						String value = forecastActual.getVaccineGroup().getVaccineCvx();
						String valueLabel = forecastActual.getVaccineGroup().getLabel();
						String valueTable = "CVX";
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
					}
					{
						obxSetId++;
						String loinc = "59783-1";
						String loincLabel = "Status in series";
						Admin admin = forecastActual.getAdmin();
						String value = admin.getAdminStatus();
						String valueLabel = admin.getLabel();
						String valueTable = "99106";
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "30981-5";
						String loincLabel = "Earliest date";
						Date value = forecastActual.getValidDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "30980-7";
						String loincLabel = "Recommended date";
						Date value = forecastActual.getDueDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
					if (forecastActual.getDueDate() != null) {
						obxSetId++;
						String loinc = "59778-1";
						String loincLabel = "Latest date";
						Date value = forecastActual.getOverdueDate();
						printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value);
					}
				}
			}
		}

		String messageResponse = sb.toString();
		recordMessageReceived(messageReceived, patientReported, messageResponse, "Query",
			categoryResponse, orgMaster);
		return messageResponse;
	}

	public String buildVxu(VaccinationReported vaccinationReported, OrgMaster orgMaster) {
		IGenericClient fhirClient = getFhirClient();
		StringBuilder sb = new StringBuilder();
		CodeMap codeMap = CodeMapManager.getCodeMap();
		Set<ProcessingFlavor> processingFlavorSet = orgMaster.getProcessingFlavorSet();
		PatientReported patientReported = vaccinationReported.getPatientReported();
		PatientMaster patientMaster = patientReported.getPatient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		HL7Reader reader = new HL7Reader(
			"MSH|^~\\&|||AIRA|IIS Sandbox|20120701082240-0500||VXU^V04^VXU_V04|NIST-IZ-001.00|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS\r");
		createMSH("VXU^V04^VXU_V04", "Z22", reader, sb, processingFlavorSet);
		printQueryPID(patientReported, processingFlavorSet, sb, patientMaster, sdf, 1);
		printQueryNK1(patientReported, sb, codeMap);

		int obxSetId = 0;
		int obsSubId = 0;
		{
			VaccinationMaster vaccination = vaccinationReported.getVaccination();
			Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
				vaccination.getVaccineCvxCode());
			if (cvxCode != null) {

				boolean originalReporter =
					vaccinationReported.getPatientReported().getOrgReported().equals(orgMaster);
				printORC(orgMaster, sb, vaccination, originalReporter);
				sb.append("RXA");
				// RXA-1
				sb.append("|0");
				// RXA-2
				sb.append("|1");
				// RXA-3
				sb.append("|").append(sdf.format(vaccination.getAdministeredDate()));
				// RXA-4
				sb.append("|");
				// RXA-5
				sb.append("|").append(cvxCode.getValue()).append("^").append(cvxCode.getLabel()).append("^CVX");
				if (!vaccinationReported.getVaccineNdcCode().equals("")) {
					Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE,
						vaccinationReported.getVaccineNdcCode());
					if (ndcCode != null) {
						sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
					}
				}
				{
					// RXA-6
					sb.append("|");
					double adminAmount = 0.0;
					if (!vaccinationReported.getAdministeredAmount().equals("")) {
						try {
							adminAmount = Double.parseDouble(vaccinationReported.getAdministeredAmount());
						} catch (NumberFormatException nfe) {
							adminAmount = 0.0;
						}
					}
					if (adminAmount > 0) {
						sb.append(adminAmount);
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
					if (vaccinationReported.getInformationSource() != null) {
						informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccinationReported.getInformationSource());
					}
					if (informationCode != null) {
						sb.append(informationCode.getValue()).append("^").append(informationCode.getLabel()).append("^NIP001");
					}
				}
				// RXA-10
				sb.append("|");
				// RXA-11
				sb.append("|");
				sb.append("^^^");
				if (vaccinationReported.getOrgLocation() == null
					|| vaccinationReported.getOrgLocation().getOrgFacilityCode() == null
					|| "".equals(vaccinationReported.getOrgLocation().getOrgFacilityCode())) {
					sb.append("AIRA");
				} else {
					sb.append(vaccinationReported.getOrgLocation().getOrgFacilityCode());
				}
				// RXA-12
				sb.append("|");
				// RXA-13
				sb.append("|");
				// RXA-14
				sb.append("|");
				// RXA-15
				sb.append("|");
				if (vaccinationReported.getLotnumber() != null) {
					sb.append(vaccinationReported.getLotnumber());
				}
				// RXA-16
				sb.append("|");
				if (vaccinationReported.getExpirationDate() != null) {
					sb.append(sdf.format(vaccinationReported.getExpirationDate()));
				}
				// RXA-17
				sb.append("|");
				sb.append(printCode(vaccinationReported.getVaccineMvxCode(),
					CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
				// RXA-18
				sb.append("|");
				sb.append(printCode(vaccinationReported.getRefusalReasonCode(),
					CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
				// RXA-19
				sb.append("|");
				// RXA-20
				sb.append("|");
				if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
					String completionStatus = vaccinationReported.getCompletionStatus();
					if (completionStatus == null || completionStatus.equals("")) {
						completionStatus = "CP";
					}
					sb.append(printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
				}

				// RXA-21
				String actionCode = vaccinationReported.getActionCode();
				if (actionCode == null || actionCode.equals("")
					|| (!actionCode.equals("A") && !actionCode.equals("D"))) {
					actionCode = "A";
				}
				sb.append("|").append(vaccinationReported.getActionCode());
				sb.append("\r");
				if (vaccinationReported.getBodyRoute() != null
					&& !vaccinationReported.getBodyRoute().equals("")) {
					sb.append("RXR");
					// RXR-1
					sb.append("|");
					sb.append(printCode(vaccinationReported.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT",
						codeMap));
					// RXR-2
					sb.append("|");
					sb.append(printCode(vaccinationReported.getBodySite(), CodesetType.BODY_SITE, "HL70163",
						codeMap));
					sb.append("\r");
				}
				TestEvent testEvent = vaccinationReported.getTestEvent();
				if (testEvent != null && testEvent.getEvaluationActualList() != null) {
					for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
						obsSubId++;
						{
							obxSetId++;
							String loinc = "30956-7";
							String loincLabel = "Vaccine type";
							String value = evaluationActual.getVaccineCvx();
							String valueLabel = evaluationActual.getVaccineCvx();
							String valueTable = "CVX";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59781-5";
							String loincLabel = "Dose validity";
							String value = evaluationActual.getDoseValid();
							String valueLabel = value;
							String valueTable = "99107";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
					}
				}

				try {
					Bundle bundle = fhirClient.search().forResource(Observation.class)
						.where(Observation.PART_OF.hasId(patientMaster.getPatientId()))
						.and(Observation.PART_OF.hasId(vaccination.getVaccinationId()))
						.returnBundle(Bundle.class).execute();
					if (bundle.hasEntry()) {
						obsSubId++;
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							ObservationReported observationReported =
								observationMapper.getReported((Observation) entry.getResource());
							obxSetId++;
							printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException e) {
				}
			}
		}
		return sb.toString();
	}

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient) {
		IGenericClient fhirClient = getFhirClient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<VaccinationMaster> vaccinationMasterList;
		{
			vaccinationMasterList = new ArrayList<>();
			Map<String, VaccinationMaster> map = new HashMap<>();
			try {
				Bundle bundle = fhirClient.search().forResource(Immunization.class)
					.where(Immunization.PATIENT.hasId(patient.getPatientId()))
					.withTag(FhirRequester.GOLDEN_SYSTEM_TAG, FhirRequester.GOLDEN_RECORD)
					.sort().ascending(Immunization.IDENTIFIER)
					.returnBundle(Bundle.class).execute();
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					Immunization immunization = (Immunization) entry.getResource();
					if (immunization.getOccurrenceDateTimeType() != null) {
						String key = sdf.format(immunization.getOccurrenceDateTimeType().getValue());
						if (immunization.getVaccineCode() != null && StringUtils.isNotBlank(immunization.getVaccineCode().getText())) {
							key += key + immunization.getVaccineCode().getText();
							VaccinationMaster vaccinationMaster = immunizationMapper.getMaster(immunization);
							map.put(key, vaccinationMaster);
						}
					}
				}
			} catch (ResourceNotFoundException e) {
			}

			List<String> keyList = new ArrayList<>(map.keySet());
			Collections.sort(keyList);
			for (String key : keyList) {
				vaccinationMasterList.add(map.get(key));
			}

		}
		return vaccinationMasterList;
	}

	public String generatePatientExternalLink() {
		IGenericClient fhirClient = getFhirClient();
		boolean keepLooking = true;
		int count = 0;
		while (keepLooking) {
			count++;
			if (count > 1000) {
				throw new RuntimeException("Unable to get a new id, tried 1000 times!");
			}
			String patientExternalLink = generateId();
			try {
				Bundle bundle = fhirClient.search().forResource(Patient.class)
					.where(Patient.IDENTIFIER.exactly().code(patientExternalLink)).returnBundle(Bundle.class).execute();
				if (!bundle.hasEntry()) {
					return patientExternalLink;
				}
			} catch (ResourceNotFoundException e) {
				return patientExternalLink;
				// we found a unique id!
			}
		}
		return null;
	}

	private Organization processSendingOrganization(HL7Reader reader) {
		Organization sendingOrganization = (Organization) fhirRequester.searchOrganization(Organization.IDENTIFIER.exactly()
			.systemAndIdentifier(reader.getValue(4, 1), reader.getValue(4, 2)));
		if (sendingOrganization == null) {
			sendingOrganization = new Organization()
				.setName(reader.getValue(4, 1))
				.addIdentifier(new Identifier()
					.setSystem(reader.getValue(4, 2))
					.setValue(reader.getValue(4, 10)));
			sendingOrganization = (Organization) fhirRequester.saveOrganization(sendingOrganization);
		}
		return sendingOrganization;
	}

	public Organization processManagingOrganization(HL7Reader reader) {
		Organization managingOrganization = null;
		String managingIdentifier = null;
		if (reader.getValue(22, 11) != null) {
			managingIdentifier = reader.getValue(22, 11);
		} else if (reader.getValue(22, 3) != null) {
			managingIdentifier = reader.getValue(22, 3);
		}
		if (managingIdentifier != null) {
			managingOrganization = (Organization) fhirRequester.searchOrganization(Organization.IDENTIFIER.exactly()
				.systemAndIdentifier(reader.getValue(22, 7), managingIdentifier));
			if (managingOrganization == null) {
				managingOrganization = new Organization();
				managingOrganization.setName(reader.getValue(22, 1));
				managingOrganization.addIdentifier()
					.setValue(managingIdentifier)
					.setSystem(reader.getValue(22, 7));
			}
		}
		if (managingOrganization != null) {
			managingOrganization = (Organization) fhirRequester.saveOrganization(managingOrganization);
		}
		return managingOrganization;
	}
}
