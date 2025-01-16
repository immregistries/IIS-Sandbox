package org.immregistries.iis.kernal.logic;


import org.apache.commons.lang3.StringUtils;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodeStatusValue;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.InternalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.SoftwareVersion;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;

public abstract class Hl7MessageWriter //extends IncomingMessageHandler
	implements IExampleMessageWriter {
	private static Integer increment = 1;


	@Autowired
	protected RepositoryClientFactory repositoryClientFactory;


	@Autowired
	ObservationMapper observationMapper;

	Random random = new Random();

	private static int nextIncrement() {
		synchronized (increment) {
			if (increment < Integer.MAX_VALUE) {
				increment = increment + 1;
			} else {
				increment = 1;
			}
			return increment;
		}
	}

	public void createMSH(String messageType, String profileId, HL7Reader reader, StringBuilder sb, Set<ProcessingFlavor> processingFlavorSet) {
		String sendingApp = "";
		String sendingFac = "";
		String receivingApp = "";
		StringBuilder receivingFac = new StringBuilder("IIS Sandbox");
		if (processingFlavorSet != null) {
			for (ProcessingFlavor processingFlavor : ProcessingFlavor.values()) {
				if (processingFlavorSet.contains(processingFlavor)) {
					receivingFac.append(" ").append(processingFlavor.getKey());
				}
			}
		}
		receivingFac.append(" v" + SoftwareVersion.VERSION);

		reader.resetPostion();
		if (reader.advanceToSegment("MSH")) {
			sendingApp = reader.getValue(3);
			sendingFac = reader.getValue(4);
			receivingApp = reader.getValue(5);
		}


		String sendingDateString;
		{
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmmssZ");
			sendingDateString = simpleDateFormat.format(new Date());
		}
		String uniqueId;
		{
			uniqueId = String.valueOf(System.currentTimeMillis()) + nextIncrement();
		}
		String production = reader.getValue(11);
		// build MSH
		sb.append("MSH|^~\\&|");
		sb.append(receivingApp).append("|");
		sb.append(receivingFac).append("|");
		sb.append(sendingApp).append("|");
		sb.append(sendingFac).append("|");
		sb.append(sendingDateString).append("|");
		sb.append("|");
		if (processingFlavorSet != null && processingFlavorSet.contains(ProcessingFlavor.MELON)) {
			int pos = messageType.indexOf("^");
			if (pos > 0) {
				messageType = messageType.substring(0, pos);
				if (System.currentTimeMillis() % 2 == 0) {
					messageType += "^ZZZ";
				}
			}
		}
		sb.append(messageType).append("|");
		sb.append(uniqueId).append("|");
		sb.append(production).append("|");
		sb.append("2.5.1|");
		sb.append("|");
		sb.append("|");
		sb.append("NE|");
		sb.append("NE|");
		sb.append("|");
		sb.append("|");
		sb.append("|");
		sb.append("|");
		sb.append(profileId).append("^CDCPHINVS\r");
	}


	public void printQueryPID(PatientMaster patientReported, Set<ProcessingFlavor> processingFlavorSet, StringBuilder sb, PatientMaster patient, SimpleDateFormat sdf, int pidCount) {
		// PID
		sb.append("PID");
		// PID-1
		sb.append("|").append(pidCount);
		// PID-2
		sb.append("|");
		// PID-3
		sb.append("|").append(patient.getExternalLink()).append("^^^IIS^SR");
		if (patientReported != null) {
			sb.append("~").append(patientReported.getExternalLink()).append("^^^").append(patientReported.getPatientReportedAuthority()).append("^").append(patientReported.getPatientReportedType());
		}
		// PID-4
		sb.append("|");
		// PID-5
		String firstName = patient.getNameFirst();
		String middleName = patient.getNameMiddle();
		String lastName = patient.getNameLast();
		String motherMaiden = null;
		if (patientReported != null) {
			motherMaiden = patientReported.getMotherMaidenName();
		}
		String dateOfBirth = sdf.format(patient.getBirthDate());

		// If "PHI" flavor, strip AIRA from names 10% of the time
		if (processingFlavorSet.contains(ProcessingFlavor.PHI)) {

			if (random.nextInt(10) == 0) {
				firstName = firstName.replace("AIRA", "");
				middleName = middleName.replace("AIRA", "");
				lastName = lastName.replace("AIRA", "");
			}
			if (motherMaiden != null) {
				motherMaiden = motherMaiden.replace("AIRA", "");
			}
		}

		if (processingFlavorSet.contains(ProcessingFlavor.CITRUS)) {
			int omission = random.nextInt(3);
			if (omission == 0) {
				firstName = "";
			} else if (omission == 1) {
				lastName = "";
			} else {
				dateOfBirth = "";
			}
		}

		sb.append("|").append(lastName).append("^").append(firstName).append("^").append(middleName).append("^^^^L");

		// PID-6
		sb.append("|");
		if (StringUtils.isNotBlank(motherMaiden)) {
			sb.append(motherMaiden).append("^^^^^^M");
		}
		// PID-7
		sb.append("|").append(dateOfBirth);
		if (patientReported != null) {
			// PID-8
			{
				String sex = patientReported.getSex();
				if (!sex.equals("F") && !sex.equals("M") && !sex.equals("X")) {
					sex = "U";
				}
				sb.append("|").append(sex);
			}
			// PID-9
			sb.append("|");
			// PID-10
			sb.append("|");
			{
				String race = patientReported.getFirstRace();
				// if processing flavor is PUNKIN then the race should be reported, and if it is null then it must be reported as UNK
				if (processingFlavorSet.contains(ProcessingFlavor.PUNKIN)) {
					CodeMap codeMap = CodeMapManager.getCodeMap();
					Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
					if (race.equals("") || raceCode == null || CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID) {
						sb.append("UNK^Unknown^CDCREC");
					} else {
						sb.append(raceCode.getValue());
						sb.append("^");
						sb.append(raceCode.getLabel());
						sb.append("^CDCREC");
					}
				} else if (StringUtils.isNotBlank(race)) {
					if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
						CodeMap codeMap = CodeMapManager.getCodeMap();
						Code raceCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_RACE, race);
						if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (raceCode != null && CodeStatusValue.getBy(raceCode.getCodeStatus()) != CodeStatusValue.VALID)) {
							sb.append(raceCode == null ? race : raceCode.getValue());
							sb.append("^");
							if (raceCode != null) {
								sb.append(raceCode.getLabel());
							}
							sb.append("^CDCREC");
						}

					}
				}
			}
			// PID-11
			sb.append("|").append(patientReported.getAddressLine1()).append("^").append(patientReported.getAddressLine2()).append("^").append(patientReported.getAddressCity()).append("^").append(patientReported.getAddressState()).append("^").append(patientReported.getAddressZip()).append("^").append(patientReported.getAddressCountry()).append("^");
			if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
				sb.append("P");
			}
			// PID-12
			sb.append("|");
			// PID-13
			sb.append("|");
			String phone = patientReported.getPhone();
			if (phone.length() == 10) {
				sb.append("^PRN^PH^^^").append(phone, 0, 3).append("^").append(phone, 3, 10);
			}
			// PID-14
			sb.append("|");
			// PID-15
			sb.append("|");
			// PID-16
			sb.append("|");
			// PID-17
			sb.append("|");
			// PID-18
			sb.append("|");
			// PID-19
			sb.append("|");
			// PID-20
			sb.append("|");
			// PID-21
			sb.append("|");
			// PID-22
			sb.append("|");
			{
				String ethnicity = patientReported.getEthnicity();
				// if processing flavor is PUNKIN then the race should be reported, and if it is null then it must be reported as UNK
				if (processingFlavorSet.contains(ProcessingFlavor.PUNKIN)) {
					CodeMap codeMap = CodeMapManager.getCodeMap();
					Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
					if (ethnicity.equals("") || ethnicityCode == null || CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID) {
						sb.append("UNK^Unknown^CDCREC");
					} else {
						sb.append(ethnicityCode.getValue());
						sb.append("^");
						sb.append(ethnicityCode.getLabel());
						sb.append("^CDCREC");
					}
				}
				if (StringUtils.isNotBlank(ethnicity)) {
					if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || processingFlavorSet.contains(ProcessingFlavor.PERSIMMON)) {
						CodeMap codeMap = CodeMapManager.getCodeMap();
						Code ethnicityCode = codeMap.getCodeForCodeset(CodesetType.PATIENT_ETHNICITY, ethnicity);
						if (processingFlavorSet.contains(ProcessingFlavor.PITAYA) || (ethnicityCode != null && CodeStatusValue.getBy(ethnicityCode.getCodeStatus()) != CodeStatusValue.VALID)) {
							sb.append(ethnicityCode == null ? ethnicity : ethnicityCode.getValue());

							sb.append("^");
							if (ethnicityCode != null) {
								sb.append(ethnicityCode.getLabel());
							}
							sb.append("^CDCREC");
						}
					}
				}
			}
			// PID-23
			sb.append("|");
			// PID-24
			sb.append("|");
			sb.append(patientReported.getBirthFlag());
			// PID-25
			sb.append("|");
			sb.append(patientReported.getBirthOrder());

		}
		sb.append("\r");
	}

	public void printORC(Tenant tenant, StringBuilder sb, VaccinationMaster vaccination, boolean originalReporter) {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		sb.append("ORC");
		// ORC-1
		sb.append("|RE");
		// ORC-2
		sb.append("|");
		if (vaccination != null) {
			sb.append(vaccination.getVaccinationId()).append("^IIS");
		}
		// ORC-3
		sb.append("|");
		if (vaccination == null) {
			if (processingFlavorSet.contains(ProcessingFlavor.LIME)) {
				sb.append("999^IIS");
			} else {
				sb.append("9999^IIS");
			}
		} else {
			if (originalReporter) {
				sb.append(vaccination.getExternalLink()).append("^").append(tenant.getOrganizationName());
			}
		}
		sb.append("\r");
	}


	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, String value) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("CE");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		sb.append(value);
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, ObservationReported ob) {
//    ObservationReported ob = observation.getObservationReported();
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append(ob.getValueType());
		// OBX-3
		sb.append("|");
		sb.append(ob.getIdentifierCode()).append("^").append(ob.getIdentifierLabel()).append("^").append(ob.getIdentifierTable());
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		if (StringUtils.isBlank(ob.getValueTable())) {
			sb.append(ob.getValueCode());
		} else {
			sb.append(ob.getValueCode()).append("^").append(ob.getValueLabel()).append("^").append(ob.getValueTable());
		}
		// OBX-6
		sb.append("|");
		if (StringUtils.isBlank(ob.getUnitsTable())) {
			sb.append(ob.getUnitsCode());
		} else {
			sb.append(ob.getUnitsCode()).append("^").append(ob.getUnitsLabel()).append("^").append(ob.getUnitsTable());
		}
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append(ob.getResultStatus());
		// OBX-12
		sb.append("|");
		// OBX-13
		sb.append("|");
		// OBX-14
		sb.append("|");
		if (ob.getObservationDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			sb.append(sdf.format(ob.getObservationDate()));
		}
		// OBX-15
		sb.append("|");
		// OBX-16
		sb.append("|");
		// OBX-17
		sb.append("|");
		if (StringUtils.isBlank(ob.getMethodTable())) {
			sb.append(ob.getMethodCode());
		} else {
			sb.append(ob.getMethodCode()).append("^").append(ob.getMethodLabel()).append("^").append(ob.getMethodTable());
		}
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, String value, String valueLabel, String valueTable) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("CE");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		sb.append("|");
		sb.append(value).append("^").append(valueLabel).append("^").append(valueTable);
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc, String loincLabel, Date value) {
		sb.append("OBX");
		// OBX-1
		sb.append("|");
		sb.append(obxSetId);
		// OBX-2
		sb.append("|");
		sb.append("DT");
		// OBX-3
		sb.append("|");
		sb.append(loinc).append("^").append(loincLabel).append("^LN");
		// OBX-4
		sb.append("|");
		sb.append(obsSubId);
		// OBX-5
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sb.append("|");
		if (value != null) {
			sb.append(sdf.format(value));
		}
		// OBX-6
		sb.append("|");
		// OBX-7
		sb.append("|");
		// OBX-8
		sb.append("|");
		// OBX-9
		sb.append("|");
		// OBX-10
		sb.append("|");
		// OBX-11
		sb.append("|");
		sb.append("F");
		sb.append("\r");
	}

	public String printCode(String value, CodesetType codesetType, String tableName, CodeMap codeMap) {
		if (value != null) {
			Code code = codeMap.getCodeForCodeset(codesetType, value);
			if (code != null) {
				if (tableName == null) {
					return code.getValue();
				}
				return code.getValue() + "^" + code.getLabel() + "^" + tableName;
			}
		}
		return "";
	}

	public void printQueryNK1(PatientMaster patientMaster, StringBuilder sb, CodeMap codeMap) {
		if (patientMaster != null) {

			for (int i = 0; i < patientMaster.getPatientGuardians().size(); i++) {
				PatientGuardian patientGuardian = patientMaster.getPatientGuardians().get(i);
				if (StringUtils.isNotBlank(patientGuardian.getGuardianRelationship()) && StringUtils.isNotBlank(patientGuardian.getName().getNameLast()) && StringUtils.isNotBlank(patientGuardian.getName().getNameFirst())) {
					Code code = codeMap.getCodeForCodeset(CodesetType.PERSON_RELATIONSHIP, patientGuardian.getGuardianRelationship());
					if (code != null) {
						sb.append("NK1");
						sb.append("|").append((i + 1));
						sb.append("|").append(patientGuardian.getName().getNameLast()).append("^").append(patientGuardian.getName().getNameFirst()).append("^^^^^L");
						sb.append("|").append(code.getValue()).append("^").append(code.getLabel()).append("^HL70063");
						sb.append("\r");
					}
				}
			}

		}
	}


}
