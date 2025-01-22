package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ValidValues;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.logic.IncomingMessageHandler.NAME_SIZE_LIMIT;

@Interceptor
@Service
public class PatientProcessingInterceptor extends AbstractLogicInterceptor {
	Logger logger = LoggerFactory.getLogger(PatientProcessingInterceptor.class);
	@Autowired
	PatientMapper patientMapper;


	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2000)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);

		IBaseResource result = requestDetails.getResource();
		logger.info("details {} {}", requestDetails.getOperation(), requestDetails.getResource().fhirType());
		if ((requestDetails.getOperation().equals("create") || requestDetails.getOperation().equals("update"))
				&& (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Patient || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Patient)) {
			PatientReported patientReported = processAndValidatePatient(patientMapper.getReported(requestDetails.getResource()), iisReportableList, processingFlavorSet);
			result = (IBaseResource) patientMapper.getFhirResource(patientReported);
		}
//			else if (requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Patient) {
//			org.hl7.fhir.r5.model.Patient patient = (org.hl7.fhir.r5.model.Patient) requestDetails.getResource();
//			List<org.hl7.fhir.r5.model.HumanName> humanNameList = new ArrayList<>(patient.getName().size());
//			for (int i = 0; i < patient.getName().size(); i++) {
//				PatientName patientName = new PatientName(patient.getName().get(i));
//				patientName = processName(patientName, processingFlavorSet);
//				if ("L".equals(patientName.getNameType())) {
//					legalName = patientName;
//				}
//				if (processingFlavorSet.contains(ProcessingFlavor.IGNORENAMETYPE)) {
//					patientName.setNameType("");
//					legalName = patientName;
//					i = patient.getName().size();
//				}
//				humanNameList.add(patientName.toR5());
//			}
//			checkLegalName(legalName, processingFlavorSet);
//			patient.setName(humanNameList);
//			requestDetails.setResource(patient);
//		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}

	public PatientReported processAndValidatePatient(PatientReported patientReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		agnosticValidation(patientReported, iisReportableList, processingFlavorSet);

		PatientName legalName = null;
		List<PatientName> patientNames = new ArrayList<>(patientReported.getPatientNames().size());
		for (int i = 0; i < patientReported.getPatientNames().size(); i++) {
			PatientName patientName = patientReported.getPatientNames().get(i);
			patientName = processName(patientName, processingFlavorSet);
			if ("L".equals(patientName.getNameType())) {
				legalName = patientName;
			}
			if (processingFlavorSet.contains(ProcessingFlavor.IGNORENAMETYPE)) {
				patientName.setNameType("");
				legalName = patientName;
				i = patientReported.getPatientNames().size();
			}
			patientNames.add(patientName);
		}
		checkLegalName(legalName, processingFlavorSet);

		patientReported.setPatientNames(patientNames);

		PatientPhone prn = null;
		for (int i = 0; i < patientReported.getPhones().size(); i++) {
			PatientPhone patientPhone = patientReported.getPhones().get(i);
			if (patientPhone != null) {
				checkPhone(patientPhone, processingFlavorSet, iisReportableList);
				if (patientPhone.getUse().equals("PRN")) {
					prn = patientPhone;
				}
			}
		}
		if (prn == null || !"PRN".equals(prn.getUse())) {
			ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
			if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
				pe.setErrorCode(IisReportableSeverity.WARN);
			}
			iisReportableList.add(IisReportable.fromProcessingException(pe));
		}
		return patientReported;
	}

	private PatientName processName(PatientName patientName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {

		String patientNameLast = patientName.getNameLast();
		String patientNameFirst = patientName.getNameFirst();
		String patientNameMiddle = patientName.getNameMiddle();
		String nameType = patientName.getNameType();

		if (processingFlavorSet.contains(ProcessingFlavor.APPLESAUCE)) {
			if (patientNameFirst.toUpperCase().contains("BABY BOY") || patientNameFirst.toUpperCase().contains("BABY GIRL") ||
				patientNameFirst.toUpperCase().contains("BABY")) {
				nameType = "NB";
			} else if (patientNameFirst.toUpperCase().contains("TEST")) {
				nameType = "TEST";
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.MANDATORYLEGALNAME)) {
			patientNameLast = patientNameLast.toUpperCase();
			patientNameFirst = patientNameFirst.toUpperCase();
			patientNameMiddle = patientNameMiddle.toUpperCase();
		}

		if (processingFlavorSet.contains(ProcessingFlavor.ASCIICONVERT)) {
			patientNameLast = Normalizer.normalize(patientNameLast, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			patientNameFirst = Normalizer.normalize(patientNameFirst, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			patientNameMiddle = Normalizer.normalize(patientNameMiddle, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		}

		if (processingFlavorSet.contains(ProcessingFlavor.NONASCIIREJECT)) {
			if (!Normalizer.normalize(patientNameLast, Normalizer.Form.NFD).contains("[^\\p{ASCII}]") ||
				!Normalizer.normalize(patientNameFirst, Normalizer.Form.NFD).contains("[^\\p{ASCII}]") ||
				!Normalizer.normalize(patientNameMiddle, Normalizer.Form.NFD).contains("[^\\p{ASCII}]")) {
				throw new ProcessingException("Illegal characters found in name", "PID", 1, 5);
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.REMOVEHYPHENSPACES)) {
			patientNameLast = patientNameLast.replace(" ", "").replace("-", "");
			patientNameFirst = patientNameFirst.replace(" ", "").replace("-", "");
			patientNameMiddle = patientNameMiddle.replace(" ", "").replace("-", "");
		}

		if (processingFlavorSet.contains(ProcessingFlavor.LIMITSIZENAME)) {
			patientNameLast = patientNameLast.substring(0, NAME_SIZE_LIMIT);
			patientNameFirst = patientNameFirst.substring(0, NAME_SIZE_LIMIT);
			patientNameMiddle = patientNameMiddle.substring(0, NAME_SIZE_LIMIT);
		}

		if (processingFlavorSet.contains(ProcessingFlavor.NOSINGLECHARNAME)) {
			if (patientNameLast.replace(".", "").length() == 1 ||
				patientNameFirst.replace(".", "").length() == 1) {
				throw new ProcessingException("Single character names not accepted", "PID", 1, 5);
			}
		}
		if (processingFlavorSet.contains(ProcessingFlavor.MIDDLENAMECONCAT)) {
			patientNameFirst += " " + patientNameMiddle;
			patientNameMiddle = "";
		}
		return new PatientName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);
	}

	private void checkLegalName(PatientName legalName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		if (legalName == null && processingFlavorSet.contains(ProcessingFlavor.MANDATORYLEGALNAME)) {
			throw new ProcessingException("Patient legal name not found", "PID", 1, 5);
		}
		String nameLast = "";
		String nameFirst = "";
		String nameMiddle = "";
		if (legalName != null) {
			nameLast = legalName.getNameLast();
			nameFirst = legalName.getNameFirst();
			nameMiddle = legalName.getNameMiddle();
			if (StringUtils.isBlank(nameLast)) {
				throw new ProcessingException("Patient last name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
			}
			if (StringUtils.isBlank(nameFirst)) {
				throw new ProcessingException("Patient first name was not found, required for accepting patient and vaccination history", "PID", 1, 5);
			}
		}
		if (legalName != null && processingFlavorSet.contains(ProcessingFlavor.REJECTLONGNAME)) {
			if (nameLast.length() > NAME_SIZE_LIMIT ||
				nameFirst.length() > NAME_SIZE_LIMIT ||
				nameMiddle.length() > NAME_SIZE_LIMIT) {
				throw new ProcessingException("Patient name is too long", "PID", 1, 5);

			}
		}


		if (legalName != null && ProcessingFlavor.MOONFRUIT.isActive() && nameFirst.startsWith("S") || nameFirst.startsWith("A")) {
			throw new ProcessingException("Immunization History cannot be stored because of patient's consent status", "PID", 0, 0, IisReportableSeverity.WARN);
		}
	}

	private void checkPhone(PatientPhone patientPhone, Set<ProcessingFlavor> processingFlavorSet, List<IisReportable> iisReportableList) {
		if (StringUtils.isNotBlank(patientPhone.getNumber())) {
//			if ("PRN".equals(patientPhone.getUse())) { // TODO specify main phone number
//				ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
//				if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
//					pe.setErrorCode(IisReportableSeverity.WARN);
//				}
//				iisReportableList.add(IisReportable.fromProcessingException(pe));
//			}
			{
				int countNums = 0;
				boolean invalidCharFound = false;
				char invalidChar = ' ';
				for (char c : patientPhone.getNumber().toCharArray()) {

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
					pe.setErrorCode(IisReportableSeverity.WARN);
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
				if (countNums != 10 || patientPhone.getNumber().startsWith("555") || patientPhone.getNumber().startsWith("0") || patientPhone.getNumber().startsWith("1")) {
					ProcessingException pe = new ProcessingException("Patient phone number does not appear to be valid", "PID", 1, 13);
					pe.setErrorCode(IisReportableSeverity.WARN);
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
			}
		}
	}

	public static void agnosticValidation(PatientReported patientReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		CodeMap codeMap = CodeMapManager.getCodeMap();
		{
			String patientSex = patientReported.getSex();
			if (!ValidValues.verifyValidValue(patientSex, ValidValues.SEX)) {
				ProcessingException pe = new ProcessingException("Patient sex '" + patientSex + "' is not recognized", "PID", 1, 8, IisReportableSeverity.WARN);
				if (processingFlavorSet.contains(ProcessingFlavor.ELDERBERRIES)) {
					pe.setErrorCode(IisReportableSeverity.WARN);
				}
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
		}

		String patientAddressCountry = patientReported.getFirstAddress().getAddressCountry();
		if (!patientAddressCountry.equals("")) {
			if (!ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_2DIGIT) && !ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_3DIGIT)) {
				ProcessingException pe = new ProcessingException("Patient address country '" + patientAddressCountry + "' is not recognized and cannot be accepted", "PID", 1, 11);
				if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
					pe.setErrorCode(IisReportableSeverity.WARN);
				}
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
		}
		if (patientAddressCountry.equals("") || patientAddressCountry.equals("US") || patientAddressCountry.equals("USA")) {
			String patientAddressState = patientReported.getFirstAddress().getAddressState();
			if (!patientAddressState.equals("")) {
				if (!ValidValues.verifyValidValue(patientAddressState, ValidValues.STATE)) {
					ProcessingException pe = new ProcessingException("Patient address state '" + patientAddressState + "' is not recognized and cannot be accepted", "PID", 1, 11);
					if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
						pe.setErrorCode(IisReportableSeverity.WARN);
					}
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
			}
		}


		{
			String race = patientReported.getFirstRace();
			if (StringUtils.isNotBlank(race)) {
				Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
				if (raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
					ProcessingException pe = new ProcessingException("Invalid race '" + race + "', message cannot be accepted", "PID", 1, 10);
					if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
						pe.setErrorCode(IisReportableSeverity.WARN);
					}
					iisReportableList.add(IisReportable.fromProcessingException(pe));
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
						pe.setErrorCode(IisReportableSeverity.WARN);
					}
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.BLACKBERRY)) {
			if (patientReported.getFirstAddress().getAddressLine1().equals("") || patientReported.getFirstAddress().getAddressCity().equals("") || patientReported.getFirstAddress().getAddressState().equals("") || patientReported.getFirstAddress().getAddressZip().equals("")) {
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
							pe.setErrorCode(IisReportableSeverity.WARN);
						}
						iisReportableList.add(IisReportable.fromProcessingException(pe));
					}
				} else if (birthFlag.equals("Y")) {
					if (birthOrder.equals("")) {
						ProcessingException pe = new ProcessingException("Multiple birth but birth order was not specified", "PID", 1, 24);
						pe.setErrorCode(IisReportableSeverity.WARN);
						iisReportableList.add(IisReportable.fromProcessingException(pe));
					} else if (!ValidValues.verifyValidValue(birthOrder, ValidValues.BIRTH_ORDER)) {
						ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not an expected value, must be between 1 and 9", "PID", 1, 25);
						if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
							pe.setErrorCode(IisReportableSeverity.WARN);
						}
						iisReportableList.add(IisReportable.fromProcessingException(pe));
					}
				} else {
					ProcessingException pe = new ProcessingException("Multiple birth indicator " + birthFlag + " is not recognized", "PID", 1, 24);
					if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
						pe.setErrorCode(IisReportableSeverity.WARN);
					}
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
			}
		}

		for (int i = 0; i < patientReported.getPatientGuardians().size(); i++) {
			PatientGuardian patientGuardian = patientReported.getPatientGuardians().get(i);
			if (StringUtils.isBlank(patientGuardian.getName().getNameLast())) {
				ProcessingException pe = new ProcessingException("Next-of-kin last name is empty", "NK1", i, 2, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
			if (StringUtils.isNotBlank(patientGuardian.getName().getNameFirst())) {
				ProcessingException pe = new ProcessingException("Next-of-kin first name is empty", "NK1", i, 2, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}

			if (StringUtils.isNotBlank(patientGuardian.getGuardianRelationship())) {
				ProcessingException pe = new ProcessingException("Next-of-kin relationship is empty", "NK1", i, 3, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
			if (patientGuardian.getGuardianRelationship().equals("MTH") || patientGuardian.getGuardianRelationship().equals("FTH") || patientGuardian.getGuardianRelationship().equals("GRD")) {
				break;
			} else {
				ProcessingException pe = new ProcessingException((StringUtils.isNotBlank(patientGuardian.getGuardianRelationship()) ? "Next-of-kin relationship not specified so is not recognized as guardian and will be ignored" : ("Next-of-kin relationship '" + patientGuardian.getGuardianRelationship() + "' is not a recognized guardian and will be ignored")), "NK1", i, 3, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
		}

		Date deathDate = patientReported.getDeathDate();
		boolean isDead = deathDate != null || StringUtils.equals(patientReported.getDeathFlag(), "Y");
		if (deathDate != null && deathDate.before(patientReported.getBirthDate())) {
			IisReportable iisReportable = err5IisReportable("2002", "Conflicting Date of Birth and Date of Death", List.of(new Hl7Location("PID-9"), new Hl7Location("PID-27")));
			iisReportableList.add(iisReportable);
		}
		if (isDead && StringUtils.equals("A", patientReported.getRegistryStatusIndicator())) {
			IisReportable iisReportable = err5IisReportable("2007", "Conflicting Patient Status and Patient Death Information", List.of(new Hl7Location("PD1-16"), new Hl7Location("PID-29"), new Hl7Location("PID-30")));
			iisReportableList.add(iisReportable);
		}
	}



}
