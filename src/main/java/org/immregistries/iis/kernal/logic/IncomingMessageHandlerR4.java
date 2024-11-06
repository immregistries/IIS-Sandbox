package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.hl7util.Reportable;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

@Service
@Conditional(OnR4Condition.class)
public class IncomingMessageHandlerR4 extends IncomingMessageHandler {

	@Override
	public String process(String message, Tenant tenant, String sendingFacilityName) {
		HL7Reader reader = new HL7Reader(message);
		String messageType = reader.getValue(9);
		String responseMessage;
		partitionCreationInterceptor.getOrCreatePartitionId(tenant.getOrganizationName());
		Set<ProcessingFlavor> processingFlavorSet = null;
		try {
			processingFlavorSet = tenant.getProcessingFlavorSet();
			String facilityId = reader.getValue(4);

			if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
				if (!facilityId.equals(tenant.getOrganizationName())) {
					throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4", "MSH", 1, 4);
				}
			}
			Organization sendingOrganization = null;
			if (StringUtils.isNotBlank(sendingFacilityName) && !sendingFacilityName.equals("null")) {
				sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_NAME, new StringParam(sendingFacilityName)));
//					Organization.NAME.matches().value(sendingFacilityName));
				if (sendingOrganization == null) {
					sendingOrganization = (Organization) fhirRequester.saveOrganization(new Organization().setName(sendingFacilityName));
				}
			}

			if (sendingOrganization == null) {
				sendingOrganization = processSendingOrganization(reader);
			}
			if (sendingOrganization == null) {
				sendingOrganization = processManagingOrganization(reader);
			}
			switch (messageType) {
				case "VXU":
					responseMessage = processVXU(tenant, reader, message, sendingOrganization);
					break;
				case "ORU":
					responseMessage = processORU(tenant, reader, message, sendingOrganization);
					break;
				case "QBP":
					responseMessage = processQBP(tenant, reader, message);
					break;
				default:
					ProcessingException pe = new ProcessingException("Unsupported message", "", 0, 0);
					List<ProcessingException> processingExceptionList = new ArrayList<>();
					processingExceptionList.add(pe);
					responseMessage = buildAck(reader, processingExceptionList, processingFlavorSet);
					recordMessageReceived(message, null, responseMessage, "Unknown", "NAck", tenant);
					break;
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
			List<ProcessingException> processingExceptionList = new ArrayList<>();
			processingExceptionList.add(new ProcessingException("Internal error prevented processing: " + e.getMessage(), null, 0, 0));
			responseMessage = buildAck(reader, processingExceptionList, processingFlavorSet);
		}
		return responseMessage;
	}

	@SuppressWarnings("unchecked")
	public String processVXU(Tenant tenant, HL7Reader reader, String message, Organization managingOrganization) throws Exception {
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = mqeMessageService.processMessage(message);
		List<Reportable> nistReportables = nistValidation(message);

		try {
			CodeMap codeMap = CodeMapManager.getCodeMap();
			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, reader, processingExceptionList, processingFlavorSet, codeMap, strictDate, null, managingOrganization);

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
				boolean rxaPresent = reader.advanceToSegment("RXA", "ORC");
				if (!rxaPresent) {
					throw new ProcessingException("RXA segment was not found after ORC segment", "ORC", orcCount, 0);
				}
				rxaCount++;
				vaccineCode = reader.getValue(5, 1);
				if (StringUtils.isBlank(vaccineCode)) {
					throw new ProcessingException("Vaccine code is not indicated in RXA-5.1", "RXA", rxaCount, 5);
				}
				if (vaccineCode.equals("998")) {
					obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported, strictDate, obxCount, null, null);
					continue;
				}
				if (StringUtils.isBlank(vaccinationReportedExternalLink)) {
					throw new ProcessingException("Vaccination order id was not found, unable to process", "ORC", orcCount, 3);
				}
				administrationDate = parseDateError(reader.getValue(3, 1), "Could not read administered date in RXA-5", "RXA", rxaCount, 3, strictDate);
				if (administrationDate.after(new Date())) {
					throw new ProcessingException("Vaccination is indicated as occuring in the future, unable to accept future vaccination events", "RXA", rxaCount, 3);
				}

				vaccinationReported = fhirRequester.searchVaccinationReported(new SearchParameterMap("identifier", new TokenParam().setValue(vaccinationReportedExternalLink)));
//						Immunization.IDENTIFIER.exactly().code(vaccinationReportedExternalLink));
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
				{
					String vaccineCodeType = reader.getValue(5, 3);
					if (vaccineCodeType.equals("NDC")) {
						vaccineNdcCode = vaccineCode;
					} else if (vaccineCodeType.equals("CPT") || vaccineCodeType.equals("C4") || vaccineCodeType.equals("C5")) {
						vaccineCptCode = vaccineCode;
					} else {
						vaccineCvxCode = vaccineCode;
					}
				}

				{
					String altVaccineCode = reader.getValue(5, 4);
					String altVaccineCodeType = reader.getValue(5, 6);
					if (!altVaccineCode.equals("")) {
						if (altVaccineCodeType.equals("NDC")) {
							if (vaccineNdcCode.equals("")) {
								vaccineNdcCode = altVaccineCode;
							}
						} else if (altVaccineCodeType.equals("CPT") || altVaccineCodeType.equals("C4") || altVaccineCodeType.equals("C5")) {
							if (StringUtils.isBlank(vaccineCptCode)) {
								vaccineCptCode = altVaccineCode;
							}
						} else {
							if (StringUtils.isBlank(vaccineCvxCode)) {
								vaccineCvxCode = altVaccineCode;
							}
						}
					}
				}

				{
					Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE, vaccineNdcCode);
					if (ndcCode != null) {
						if (ndcCode.getCodeStatus() != null && ndcCode.getCodeStatus().getDeprecated() != null && ndcCode.getCodeStatus().getDeprecated().getNewCodeValue() != null && !ndcCode.getCodeStatus().getDeprecated().getNewCodeValue().equals("")) {
							vaccineNdcCode = ndcCode.getCodeStatus().getDeprecated().getNewCodeValue();
						}
						Code cvxCode = codeMap.getRelatedCode(ndcCode, CodesetType.VACCINATION_CVX_CODE);
						if (cvxCode == null) {
							ProcessingException pe = new ProcessingException("Unrecognized NDC " + vaccineNdcCode, "RXA", rxaCount, 5).setWarning();
							processingExceptionList.add(pe);
						} else {
							if (vaccineCvxCode.equals("")) {
								vaccineCvxCode = cvxCode.getValue();
							} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
								// NDC doesn't map to the CVX code that was submitted!
								ProcessingException pe = new ProcessingException("NDC " + vaccineNdcCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5);
								pe.setWarning();
								processingExceptionList.add(pe);
							}
						}
					}
				}
				{
					Code cptCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CPT_CODE, vaccineCptCode);
					if (cptCode != null) {
						Code cvxCode = codeMap.getRelatedCode(cptCode, CodesetType.VACCINATION_CVX_CODE);
						if (cvxCode == null) {
							ProcessingException pe = new ProcessingException("Unrecognized CPT " + cptCode, "RXA", rxaCount, 5).setWarning();
							processingExceptionList.add(pe);
						} else {
							if (vaccineCvxCode.equals("")) {
								vaccineCvxCode = cvxCode.getValue();
							} else if (!vaccineCvxCode.equals(cvxCode.getValue())) {
								// CPT doesn't map to the CVX code that was submitted!
								ProcessingException pe = new ProcessingException("CPT " + vaccineCptCode + " maps to " + cvxCode.getValue() + " but CVX " + vaccineCvxCode + " was also reported, preferring CVX code", "RXA", rxaCount, 5).setWarning();
								processingExceptionList.add(pe);
							}
						}
					}
				}
				if (vaccineCvxCode.equals("")) {
					throw new ProcessingException("Unable to find a recognized vaccine administration code (CVX, NDC, or CPT)", "RXA", rxaCount, 5);
				} else {
					Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE, vaccineCvxCode);
					if (cvxCode != null) {
						vaccineCvxCode = cvxCode.getValue();
					} else {
						throw new ProcessingException("Unrecognized CVX vaccine '" + vaccineCvxCode + "'", "RXA", rxaCount, 5);
					}

				}


				{
					String administeredAtLocation = reader.getValue(11, 4);
					if (StringUtils.isEmpty(administeredAtLocation)) {

					}
					if (StringUtils.isNotEmpty(administeredAtLocation)) {
						OrgLocation orgLocation = fhirRequester.searchOrgLocation(new SearchParameterMap(Location.SP_IDENTIFIER, new TokenParam().setValue(administeredAtLocation)));
//								Location.IDENTIFIER.exactly().code(administeredAtLocation));

						if (orgLocation == null) {
							if (processingFlavorSet.contains(ProcessingFlavor.PEAR)) {
								throw new ProcessingException("Unrecognized administered at location, unable to accept immunization report", "RXA", rxaCount, 11);
							}
							orgLocation = new OrgLocation();
							orgLocation.setOrgFacilityCode(administeredAtLocation);
							orgLocation.setTenant(tenant);
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
						ModelPerson modelPerson = fhirRequester.searchPractitioner(new SearchParameterMap(Practitioner.SP_IDENTIFIER, new TokenParam().setValue(administeringProvider)));
//								Practitioner.IDENTIFIER.exactly().code(administeringProvider));
						if (modelPerson == null) {
							modelPerson = new ModelPerson();
							modelPerson.setPersonExternalLink(administeringProvider);
							modelPerson.setTenant(tenant);
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
				vaccinationReported.setExpirationDate(parseDateWarn(reader.getValue(16), "Invalid vaccination expiration date", "RXA", rxaCount, 16, strictDate, processingExceptionList));
				vaccinationReported.setVaccineMvxCode(reader.getValue(17));
				vaccinationReported.setRefusalReasonCode(reader.getValue(18));
				vaccinationReported.setCompletionStatus(reader.getValue(20));

				if (StringUtils.isNotBlank(vaccinationReported.getRefusalReasonCode())) {
					Code refusalCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_REFUSAL, vaccinationReported.getRefusalReasonCode());
					if (refusalCode == null) {
						ProcessingException pe = new ProcessingException("Unrecognized refusal reason", "RXA", rxaCount, 18);
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
						throw new ProcessingException("RXR segment is required for administered vaccinations", "RXA", rxaCount, 0);
					}
				}
				if (vaccinationReported.getAdministeredDate().before(patientReported.getBirthDate()) && !processingFlavorSet.contains(ProcessingFlavor.CLEMENTINE)) {
					throw new ProcessingException("Vaccination is reported as having been administered before the patient was born", "RXA", rxaCount, 3);
				}
				if (!vaccinationReported.getVaccineCvxCode().equals("998") && !vaccinationReported.getVaccineCvxCode().equals("999") && (vaccinationReported.getCompletionStatus().equals("CP") || vaccinationReported.getCompletionStatus().equals("PA") || vaccinationReported.getCompletionStatus().equals(""))) {
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
							Code fundingEligibilityCode = codeMap.getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, fundingEligibility);
							if (fundingEligibilityCode == null) {
								ProcessingException pe = new ProcessingException("Funding eligibility '" + fundingEligibility + "' was not recognized", "OBX", tempObxCount, 5).setWarning();
								processingExceptionList.add(pe);
							} else {
								vaccinationReported.setFundingEligibility(fundingEligibilityCode.getValue());
							}
						}
					} else if (indicator.equals("30963-3")) {
						String fundingSource = reader.getValue(5);
						if (!fundingSource.equals("")) {
							Code fundingSourceCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, fundingSource);
							if (fundingSourceCode == null) {
								ProcessingException pe = new ProcessingException("Funding source '" + fundingSource + "' was not recognized", "OBX", tempObxCount, 5).setWarning();
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
				obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported, strictDate, obxCount, vaccinationReported, null);

			}
			if (processingFlavorSet.contains(ProcessingFlavor.CRANBERRY) && vaccinationCount == 0) {
				throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered or historical vaccination specified", "", 0, 0);
			}
			if (processingFlavorSet.contains(ProcessingFlavor.BILBERRY) && (vaccinationCount == 0 && refusalCount == 0)) {
				throw new ProcessingException("Patient vaccination history cannot be accepted without at least one administered, historical, or refused vaccination specified", "", 0, 0);
			}
			String ack = buildAckMqe(mqeMessageServiceResponse, processingExceptionList, processingFlavorSet, nistReportables);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			if (!processingExceptionList.contains(e)) {
				processingExceptionList.add(e);
			}
			String ack = buildAckMqe(mqeMessageServiceResponse, processingExceptionList, processingFlavorSet, nistReportables);
			recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}

	}

	@SuppressWarnings("unchecked")
	public PatientReported processPatient(Tenant tenant, HL7Reader reader, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, PatientReported patientReported, Organization managingOrganization) throws ProcessingException {
		String patientReportedExternalLink = "";
		String patientReportedAuthority = "";
		String patientReportedType = "MR";
		if (reader.advanceToSegment("PID")) {
			patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
			patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
			if (patientReportedExternalLink.equals("")) {
				patientReportedAuthority = "";
				patientReportedType = "PT";
				patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
				patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
				if (patientReportedExternalLink.equals("")) {
					patientReportedAuthority = "";
					patientReportedType = "PI";
					patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
					patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
					if (patientReportedExternalLink.equals("")) {
						throw new ProcessingException("MRN was not found, required for accepting vaccination report", "PID", 1, 3);
					}
				}
			}
		} else {
			throw new ProcessingException("No PID segment found, required for accepting vaccination report", "", 0, 0);
		}
		patientReported = fhirRequester.searchPatientReported(new SearchParameterMap("identifier", new TokenParam().setValue(patientReportedExternalLink)));

		if (patientReported == null) {
			patientReported = new PatientReported();
			patientReported.setTenant(tenant);
			patientReported.setExternalLink(patientReportedExternalLink);
			patientReported.setReportedDate(new Date());
			if (managingOrganization != null) {
				patientReported.setManagingOrganizationId("Organization/" + managingOrganization.getIdElement().getIdPart());
			}
		}


		{

			if (processingFlavorSet.contains(ProcessingFlavor.APPLESAUCE)) {

			}
			List<PatientName> names = new ArrayList<>(reader.getRepeatCount(5));
			PatientName legalName = null;
			for (int i = 0; i < reader.getRepeatCount(5); i++) {
				String patientNameLast = reader.getValueRepeat(5, 1, i);
				String patientNameFirst = reader.getValueRepeat(5, 2, i);
				String patientNameMiddle = reader.getValueRepeat(5, 3, i);

				String nameType = reader.getValueRepeat(5, 7, i);
				if (processingFlavorSet.contains(ProcessingFlavor.APPLESAUCE)) {
					if (patientNameFirst.toUpperCase().contains("BABY BOY") || patientNameFirst.toUpperCase().contains("BABY GIRL") ||
						patientNameFirst.toUpperCase().contains("BABY")) {
						nameType = "NB";
					} else if (patientNameFirst.toUpperCase().contains("TEST")) {
						nameType = "TEST";
					}
				}
				PatientName patientName = new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);
				names.add(patientName);
				if (nameType.equals("L")) {
					legalName = patientName;
				}
			}

			String patientPhone = reader.getValue(13, 6) + reader.getValue(13, 7);
			String telUseCode = reader.getValue(13, 2);
			if (patientPhone.length() > 0) {
				if (!telUseCode.equals("PRN")) {
					ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
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
						ProcessingException pe = new ProcessingException("Patient phone number has unexpected character: " + invalidChar, "PID", 1, 13);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
					if (countNums != 10 || patientPhone.startsWith("555") || patientPhone.startsWith("0") || patientPhone.startsWith("1")) {
						ProcessingException pe = new ProcessingException("Patient phone number does not appear to be valid", "PID", 1, 13);
						pe.setWarning();
						processingExceptionList.add(pe);
					}
				}
			}
			if (!telUseCode.equals("PRN")) {
				patientPhone = "";
			}

			if (legalName == null && processingFlavorSet.contains(ProcessingFlavor.MANDATORYLEGALNAME)) {
				throw new ProcessingException("Patient legal name not found", "PID", 1, 5);
			}
			if (legalName != null && legalName.getNameLast().equals("")) {
				throw new ProcessingException("Patient last name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
			}
			if (legalName != null && legalName.getNameFirst().equals("")) {
				throw new ProcessingException("Patient first name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
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
			patientBirthDate = parseDateError(reader.getValue(7), "Bad format for date of birth", "PID", 1, 7, strictDate);
			if (patientBirthDate.after(new Date())) {
				throw new ProcessingException("Patient is indicated as being born in the future, unable to record patients who are not yet born", "PID", 1, 7);
			}
			patientReported.setExternalLink(patientReportedExternalLink);
			patientReported.setPatientReportedType(patientReportedType);
			patientReported.setPatientNames(names);
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
			patientReported.setDeathDate(parseDateWarn(reader.getValue(29), "Invalid patient death date", "PID", 1, 29, strictDate, processingExceptionList));
			patientReported.setDeathFlag(reader.getValue(30));
			patientReported.setEmail(reader.getValueBySearchingRepeats(13, 4, "NET", 2));
			patientReported.setPhone(patientPhone);
			patientReported.setPatientReportedAuthority(patientReportedAuthority);

			{
				String patientSex = patientReported.getSex();
				if (!ValidValues.verifyValidValue(patientSex, ValidValues.SEX)) {
					ProcessingException pe = new ProcessingException("Patient sex '" + patientSex + "' is not recognized", "PID", 1, 8).setWarning();
					if (processingFlavorSet.contains(ProcessingFlavor.ELDERBERRIES)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}

			String patientAddressCountry = patientReported.getAddressCountry();
			if (!patientAddressCountry.equals("")) {
				if (!ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_2DIGIT) && !ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_3DIGIT)) {
					ProcessingException pe = new ProcessingException("Patient address country '" + patientAddressCountry + "' is not recognized and cannot be accepted", "PID", 1, 11);
					if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
						pe.setWarning();
					}
					processingExceptionList.add(pe);
				}
			}
			if (patientAddressCountry.equals("") || patientAddressCountry.equals("US") || patientAddressCountry.equals("USA")) {
				String patientAddressState = patientReported.getAddressState();
				if (!patientAddressState.equals("")) {
					if (!ValidValues.verifyValidValue(patientAddressState, ValidValues.STATE)) {
						ProcessingException pe = new ProcessingException("Patient address state '" + patientAddressState + "' is not recognized and cannot be accepted", "PID", 1, 11);
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
					if (raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
						ProcessingException pe = new ProcessingException("Invalid race '" + race + "', message cannot be accepted", "PID", 1, 10);
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
					if (ethnicityCode == null || CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
						ProcessingException pe = new ProcessingException("Invalid ethnicity '" + ethnicity + "', message cannot be accepted", "PID", 1, 10);
						if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
							pe.setWarning();
						}
						processingExceptionList.add(pe);
					}
				}
			}

			if (processingFlavorSet.contains(ProcessingFlavor.BLACKBERRY)) {
				if (patientReported.getAddressLine1().equals("") || patientReported.getAddressCity().equals("") || patientReported.getAddressState().equals("") || patientReported.getAddressZip().equals("")) {
					throw new ProcessingException("Patient address is required but it was not sent", "PID", 1, 11);
				}
			}

			{
				String birthFlag = patientReported.getBirthFlag();
				String birthOrder = patientReported.getBirthOrder();
				if (!birthFlag.equals("") || !birthOrder.equals("")) {
					if (birthFlag.equals("") || birthFlag.equals("N")) {
						// The only acceptable value here is now blank or 1
						if (!birthOrder.equals("1") && !birthOrder.equals("")) {
							ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not indicated as multiple birth", "PID", 1, 25);
							if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
								pe.setWarning();
							}
							processingExceptionList.add(pe);
						}
					} else if (birthFlag.equals("Y")) {
						if (birthOrder.equals("")) {
							ProcessingException pe = new ProcessingException("Multiple birth but birth order was not specified", "PID", 1, 24);
							pe.setWarning();
							processingExceptionList.add(pe);
						} else if (!ValidValues.verifyValidValue(birthOrder, ValidValues.BIRTH_ORDER)) {
							ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not an expected value, must be between 1 and 9", "PID", 1, 25);
							if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
								pe.setWarning();
							}
							processingExceptionList.add(pe);
						}
					} else {
						ProcessingException pe = new ProcessingException("Multiple birth indicator " + birthFlag + " is not recognized", "PID", 1, 24);
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
			patientReported.setProtectionIndicatorDate(parseDateWarn(reader.getValue(13), "Invalid protection indicator date", "PD1", 1, 13, strictDate, processingExceptionList));
			patientReported.setRegistryStatusIndicator(reader.getValue(16));
			patientReported.setRegistryStatusIndicatorDate(parseDateWarn(reader.getValue(17), "Invalid registry status indicator date", "PD1", 1, 17, strictDate, processingExceptionList));
			patientReported.setPublicityIndicatorDate(parseDateWarn(reader.getValue(18), "Invalid publicity indicator date", "PD1", 1, 18, strictDate, processingExceptionList));
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
					ProcessingException pe = new ProcessingException("Next-of-kin last name is empty", "NK1", repeatCount, 2).setWarning();
					processingExceptionList.add(pe);
				}
				if (patientReported.getGuardianFirst().equals("")) {
					ProcessingException pe = new ProcessingException("Next-of-kin first name is empty", "NK1", repeatCount, 2).setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("")) {
					ProcessingException pe = new ProcessingException("Next-of-kin relationship is empty", "NK1", repeatCount, 3).setWarning();
					processingExceptionList.add(pe);
				}
				if (guardianRelationship.equals("MTH") || guardianRelationship.equals("FTH") || guardianRelationship.equals("GRD")) {
					break;
				} else {
					ProcessingException pe = new ProcessingException((guardianRelationship.equals("") ? "Next-of-kin relationship not specified so is not recognized as guardian and will be ignored" : ("Next-of-kin relationship '" + guardianRelationship + "' is not a recognized guardian and will be ignored")), "NK1", repeatCount, 3).setWarning();
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

	public String processORU(Tenant tenant, HL7Reader reader, String message, Organization managingOrganization) {
		List<ProcessingException> processingExceptionList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			CodeMap codeMap = CodeMapManager.getCodeMap();

			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, reader, processingExceptionList, processingFlavorSet, codeMap, strictDate, null, managingOrganization);

			int orcCount = 0;
			int obxCount = 0;
			while (reader.advanceToSegment("ORC")) {
				orcCount++;
				if (reader.advanceToSegment("OBR", "ORC")) {
					obxCount = readAndCreateObservations(reader, processingExceptionList, patientReported, strictDate, obxCount, null, null);
				} else {
					throw new ProcessingException("OBR segment was not found after ORC segment", "ORC", orcCount, 0);
				}
			}
			String ack = buildAck(reader, processingExceptionList, processingFlavorSet);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			if (!processingExceptionList.contains(e)) {
				processingExceptionList.add(e);
			}
			String ack = buildAck(reader, processingExceptionList, processingFlavorSet);
			recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}
	}

	@SuppressWarnings("unchecked")
	public ObservationReported readObservations(HL7Reader reader, List<ProcessingException> processingExceptionList, PatientReported patientReported, boolean strictDate, int obxCount, VaccinationReported vaccinationReported, VaccinationMaster vaccination, String identifierCode, String valueCode) {
//    ObservationMaster observationMaster = null;
		ObservationReported observationReported = null;
		if (vaccination == null) {
			observationReported = fhirRequester.searchObservationReported(new SearchParameterMap(Observation.SP_PART_OF, new ReferenceParam().setMissing(true)).add(Observation.SP_SUBJECT, new ReferenceParam(patientReported.getPatientId())));
//				Observation.PART_OF.isMissing(true),
//				Observation.SUBJECT.hasId(patientReported.getPatientId()));
		} else {
			observationReported = fhirRequester.searchObservationReported(new SearchParameterMap(Observation.SP_PART_OF, new ReferenceParam(vaccination.getVaccinationId())).add(Observation.SP_SUBJECT, new ReferenceParam(patientReported.getPatientId())));
//				Observation.PART_OF.hasId(vaccination.getVaccinationId()),
//				Observation.SUBJECT.hasId(patientReported.getPatientId()));
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
		observationReported.setObservationDate(parseDateWarn(reader.getValue(14), "Unparsable date/time of observation", "OBX", obxCount, 14, strictDate, processingExceptionList));
		observationReported.setMethodCode(reader.getValue(17, 1));
		observationReported.setMethodLabel(reader.getValue(17, 2));
		observationReported.setMethodTable(reader.getValue(17, 3));
		return observationReported;
	}

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<VaccinationMaster> vaccinationMasterList;
		{
			vaccinationMasterList = new ArrayList<>();
			Map<String, VaccinationMaster> map = new HashMap<>();
			try {
				Bundle bundle = fhirClient.search().forResource(Immunization.class).where(Immunization.PATIENT.hasId(patient.getPatientId())).withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).sort().ascending(Immunization.IDENTIFIER).returnBundle(Bundle.class).execute();
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

	private Organization processSendingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(4, 1);
		Organization sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_IDENTIFIER, new TokenParam().setSystem(reader.getValue(4, 10)).setValue(reader.getValue(4, 2))));
//			Organization.IDENTIFIER.exactly()
//			.systemAndIdentifier(reader.getValue(4, 10), reader.getValue(4, 2)));
		if (sendingOrganization == null && StringUtils.isNotBlank(organizationName)) {
			sendingOrganization = new Organization().setName(organizationName).addIdentifier(new Identifier().setSystem(reader.getValue(4, 2)).setValue(reader.getValue(4, 10)));
			sendingOrganization = (Organization) fhirRequester.saveOrganization(sendingOrganization);
		}
		return sendingOrganization;
	}

	public Organization processManagingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(22, 1);
		Organization managingOrganization = null;
		String managingIdentifier = null;
		if (StringUtils.isNotBlank(reader.getValue(22, 11))) {
			managingIdentifier = reader.getValue(22, 11);
		} else if (StringUtils.isNotBlank(reader.getValue(22, 3))) {
			managingIdentifier = reader.getValue(22, 3);
		}
		if (managingIdentifier != null) {
			managingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_IDENTIFIER, new TokenParam().setSystem(reader.getValue(22, 7)).setValue(managingIdentifier)));
//				Organization.IDENTIFIER.exactly()
//				.systemAndIdentifier(reader.getValue(22, 7), managingIdentifier));
			if (managingOrganization == null) {
				managingOrganization = new Organization();
				managingOrganization.setName(organizationName);
				managingOrganization.addIdentifier().setValue(managingIdentifier).setSystem(reader.getValue(22, 7));
			}
		}
		if (managingOrganization != null) {
			managingOrganization = (Organization) fhirRequester.saveOrganization(managingOrganization);
		}
		return managingOrganization;
	}
}
