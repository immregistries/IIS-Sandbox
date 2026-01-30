package org.immregistries.iis.kernal.logic.validation;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.logic.hl7v2.ack.IisReportableUtilService;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.ModelName;
import org.immregistries.iis.kernal.model.ModelPhone;
import org.immregistries.iis.kernal.model.PatientGuardian;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverityLevel;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
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

import static org.immregistries.iis.kernal.logic.hl7v2.handling.IIncomingMessageHandler.NAME_SIZE_LIMIT;

@Service
public class PatientValidator extends IisValidator {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PatientMapper patientMapper;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	IisReportableUtilService iisReportableUtilService;


	public PatientReported processAndValidatePatient(PatientReported patientReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		testMapping(patientMapper, patientReported);

		if (patientReported.getBirthDate() != null && patientReported.getBirthDate().after(new Date())) {
			throw new ProcessingException("Patient is indicated as being born in the future, unable to record patients who are not yet born", "PID", 1, 7);
		}
		ModelName legalName = null;
		List<ModelName> modelNames = new ArrayList<>(patientReported.getPatientNames().size());
		for (int i = 0; i < patientReported.getPatientNames().size(); i++) {
			ModelName modelName = patientReported.getPatientNames().get(i);
			modelName = processName(modelName, processingFlavorSet);
			if ("L".equals(modelName.getNameType())) {
				legalName = modelName;
			}
			if (processingFlavorSet.contains(ProcessingFlavor.IGNORENAMETYPE)) {
				modelName.setNameType("");
				legalName = modelName;
				i = patientReported.getPatientNames().size();
			}
			modelNames.add(modelName);
		}
		checkLegalName(legalName, processingFlavorSet);

		patientReported.setPatientNames(modelNames);

		ModelPhone prn = null;
		for (int i = 0; i < patientReported.getPhones().size(); i++) {
			ModelPhone patientPhone = patientReported.getPhones().get(i);
			if (patientPhone != null) {
				checkPhone(patientPhone, processingFlavorSet, iisReportableList);
				if ("PRN".equals(patientPhone.getUse())) {
					prn = patientPhone;
				}
			}
		}
		if (prn == null || !"PRN".equals(prn.getUse())) {
			ProcessingException pe = new ProcessingException("Patient phone telecommunication type must be PRN ", "PID", 1, 13);
			if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
				pe.setErrorCode(IisReportableSeverityLevel.WARN);
			}
			iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
		}
		agnosticValidation(patientReported, iisReportableList, processingFlavorSet);
		return patientReported;
	}

	private ModelName processName(ModelName modelName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {

		String patientNameLast = modelName.getNameLast();
		String patientNameFirst = modelName.getNameFirst();
		String patientNameMiddle = modelName.getNameMiddle();
		String nameType = modelName.getNameType();

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
		return new ModelName(patientNameLast, patientNameFirst, patientNameMiddle, nameType);
	}

	private void checkLegalName(ModelName legalName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
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
		if (legalName != null && processingFlavorSet.contains(ProcessingFlavor.MOONFRUIT) && (nameFirst.startsWith("S") || nameFirst.startsWith("A"))) {
			throw new ProcessingException("Immunization History cannot be stored because of patient's consent status", "PID", 0, 0, IisReportableSeverityLevel.WARN);
		}
	}

	private void checkPhone(ModelPhone patientPhone, Set<ProcessingFlavor> processingFlavorSet, List<IisReportable> iisReportableList) {
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
					pe.setErrorCode(IisReportableSeverityLevel.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
				if (countNums != 10 || patientPhone.getNumber().startsWith("555") || patientPhone.getNumber().startsWith("0") || patientPhone.getNumber().startsWith("1")) {
					ProcessingException pe = new ProcessingException("Patient phone number does not appear to be valid", "PID", 1, 13);
					pe.setErrorCode(IisReportableSeverityLevel.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
		}
	}

	public void agnosticValidation(PatientReported patientReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		CodeMap codeMap = codeMapManagerService.getCodeMap();
		{
			String patientSex = patientReported.getSex();
			if (!ValidValues.verifyValidValue(patientSex, ValidValues.SEX)) {
				ProcessingException pe = new ProcessingException("Patient sex '" + patientSex + "' is not recognized", "PID", 1, 8, IisReportableSeverityLevel.WARN);
				if (processingFlavorSet.contains(ProcessingFlavor.ELDERBERRIES)) {
					pe.setErrorCode(IisReportableSeverityLevel.WARN);
				}
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
		}

		if (!patientReported.getAddresses().isEmpty()) {
			String patientAddressCountry = patientReported.getFirstAddress().getAddressCountry();
			if (StringUtils.isNotBlank(patientAddressCountry)) {
				if (!ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_2DIGIT) && !ValidValues.verifyValidValue(patientAddressCountry, ValidValues.COUNTRY_3DIGIT)) {
					ProcessingException pe = new ProcessingException("Patient address country '" + patientAddressCountry + "' is not recognized and cannot be accepted", "PID", 1, 11);
					if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
						pe.setErrorCode(IisReportableSeverityLevel.WARN);
					}
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
			if (StringUtils.isBlank(patientAddressCountry) || "US".equals(patientAddressCountry) || "USA".equals(patientAddressCountry)) {
				String patientAddressState = patientReported.getFirstAddress().getAddressState();
				if (StringUtils.isNotBlank(patientAddressState)) {
					if (!ValidValues.verifyValidValue(patientAddressState, ValidValues.STATE)) {
						ProcessingException pe = new ProcessingException("Patient address state '" + patientAddressState + "' is not recognized and cannot be accepted", "PID", 1, 11);
						if (processingFlavorSet.contains(ProcessingFlavor.GUAVA)) {
							pe.setErrorCode(IisReportableSeverityLevel.WARN);
						}
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					}
				}
			}
		}

		for (String race : patientReported.getRaces()) {
			if (StringUtils.isNotBlank(race)) {
				Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
				if (raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
					ProcessingException pe = new ProcessingException("Invalid race '" + race + "', message cannot be accepted", "PID", 1, 10);
					if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
						pe.setErrorCode(IisReportableSeverityLevel.WARN);
					}
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
		}


		{
			String ethnicity = patientReported.getEthnicity();
			if (StringUtils.isNotBlank(ethnicity)) {
				Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
				if (ethnicityCode == null || CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
					ProcessingException pe = new ProcessingException("Invalid ethnicity '" + ethnicity + "', message cannot be accepted", "PID", 1, 10);
					if (!processingFlavorSet.contains(ProcessingFlavor.FIG)) {
						pe.setErrorCode(IisReportableSeverityLevel.WARN);
					}
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.BLACKBERRY)) {
			if (StringUtils.isBlank(patientReported.getFirstAddress().getAddressLine1()) || StringUtils.isBlank(patientReported.getFirstAddress().getAddressCity()) || StringUtils.isBlank(patientReported.getFirstAddress().getAddressState()) || StringUtils.isBlank(patientReported.getFirstAddress().getAddressZip())) {
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
							pe.setErrorCode(IisReportableSeverityLevel.WARN);
						}
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					}
				} else if (birthFlag.equals("Y")) {
					if (birthOrder.equals("")) {
						ProcessingException pe = new ProcessingException("Multiple birth but birth order was not specified", "PID", 1, 24);
						pe.setErrorCode(IisReportableSeverityLevel.WARN);
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					} else if (!ValidValues.verifyValidValue(birthOrder, ValidValues.BIRTH_ORDER)) {
						ProcessingException pe = new ProcessingException("Birth order was specified as " + birthOrder + " but not an expected value, must be between 1 and 9", "PID", 1, 25);
						if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
							pe.setErrorCode(IisReportableSeverityLevel.WARN);
						}
						iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
					}
				} else {
					ProcessingException pe = new ProcessingException("Multiple birth indicator " + birthFlag + " is not recognized", "PID", 1, 24);
					if (processingFlavorSet.contains(ProcessingFlavor.PLANTAIN)) {
						pe.setErrorCode(IisReportableSeverityLevel.WARN);
					}
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
		}

		for (int i = 0; i < patientReported.getPatientGuardians().size(); i++) {
			PatientGuardian patientGuardian = patientReported.getPatientGuardians().get(i);
			if (StringUtils.isBlank(patientGuardian.getName().getNameLast())) {
				ProcessingException pe = new ProcessingException("Next-of-kin last name is empty", "NK1", i, 2, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
			if (StringUtils.isBlank(patientGuardian.getName().getNameFirst())) {
				ProcessingException pe = new ProcessingException("Next-of-kin first name is empty", "NK1", i, 2, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}

			if (StringUtils.isBlank(patientGuardian.getGuardianRelationship())) {
				ProcessingException pe = new ProcessingException("Next-of-kin relationship is empty", "NK1", i, 3, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
			if ("MTH".equals(patientGuardian.getGuardianRelationship()) || "FTH".equals(patientGuardian.getGuardianRelationship()) || "GRD".equals(patientGuardian.getGuardianRelationship())) {
				break;
			} else {
				ProcessingException pe = new ProcessingException((StringUtils.isNotBlank(patientGuardian.getGuardianRelationship()) ? "Next-of-kin relationship not specified so is not recognized as guardian and will be ignored" : ("Next-of-kin relationship '" + patientGuardian.getGuardianRelationship() + "' is not a recognized guardian and will be ignored")), "NK1", i, 3, IisReportableSeverityLevel.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
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
