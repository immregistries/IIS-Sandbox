package org.immregistries.iis.kernal.logic;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.model.PatientGuardian;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class AgnosticValidator {
	public static void doChecks(PatientReported patientReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
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

		String patientAddressCountry = patientReported.getAddressCountry();
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
			String patientAddressState = patientReported.getAddressState();
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
			String race = patientReported.getRace();
			if (!race.equals("")) {
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
			IisReportable iisReportable = getIisReportable("2002", "Conflicting Date of Birth and Date of Death", List.of(new Hl7Location("PID-9"), new Hl7Location("PID-27")));
			iisReportableList.add(iisReportable);
		}
		if (isDead && StringUtils.equals("A", patientReported.getRegistryStatusIndicator())) {
			IisReportable iisReportable = getIisReportable("2007", "Conflicting Patient Status and Patient Death Information", List.of(new Hl7Location("PD1-16"), new Hl7Location("PID-29"), new Hl7Location("PID-30")));
			iisReportableList.add(iisReportable);
		}
	}

	private static @NotNull IisReportable getIisReportable(String number, String text, List<@NotNull Hl7Location> hl7LocationList) {
		IisReportable iisReportable = new IisReportable();
		iisReportable.setHl7LocationList(hl7LocationList);
		CodedWithExceptions applicationErrorCode = new CodedWithExceptions("ERR-5");
		applicationErrorCode.setIdentifier(number);
		applicationErrorCode.setText(text);
		iisReportable.setApplicationErrorCode(applicationErrorCode);
		iisReportable.setHl7ErrorCode(applicationErrorCode);
		iisReportable.setSeverity(IisReportableSeverity.WARN);
		return iisReportable;
	}
}
