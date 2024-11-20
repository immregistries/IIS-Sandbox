package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.smm.tester.manager.HL7Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Experimental, to help with improving mapping definition
 */
public class VxuToBundleExperiment {
	public Bundle convert(String message,
								 List<ProcessingException> processingExceptionList,
								 Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		HL7Reader reader = new HL7Reader(message);
		String messageType = reader.getValue(9);
		String responseMessage;
		if (messageType != "VXU") {
			return null;
		}
		//		Organization sendingOrganization = processSendingOrganization(reader);
		Organization managingOrganization = processManagingOrganization(reader);
		List<Identifier> identifierList = new ArrayList<>();
		if (reader.advanceToSegment("PID")) {
			String patientType;
			for (String type : new String[]{"MR", "PT", "PI"}) {
				String identifierValue = reader.getValueBySearchingRepeats(3, 1, type, 5);
				if (!identifierValue.isBlank()) {
					identifierList.add(new Identifier().setValue(identifierValue)
						.setSystem(reader.getValueBySearchingRepeats(3, 4, type, 5))
						.setType(new CodeableConcept(new Coding()
							.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
							.setCode(type)))
					);
				}
			}
			if (identifierList.isEmpty()) {
				throw new ProcessingException(
					"MRN was not found, required for accepting vaccination report", "PID", 1, 3);
			}
		} else {
			throw new ProcessingException(
				"No PID segment found, required for accepting vaccination report", "", 0, 0);
		}

		// TODO SEARCH ?
		Patient patient = new Patient();
		patient.setIdentifier(identifierList);
		patient.setManagingOrganization(new Reference(managingOrganization));

		HumanName name = new HumanName();
		name.setFamily(reader.getValue(5, 1));
		name.addGiven(reader.getValue(5, 2));
		name.addGiven(reader.getValue(5, 3));
		if (name.getFamily().isBlank()) {
			throw new ProcessingException(
				"Patient last name was not found, required for accepting patient and vaccination history",
				"PID", 1, 5);
		}
		if (name.getGivenAsSingleString().isBlank()) {
			throw new ProcessingException(
				"Patient first name was not found, required for accepting patient and vaccination history",
				"PID", 1, 5);
		}
		patient.addName(name);

		ContactPoint phone = new ContactPoint()
			.setSystem(ContactPoint.ContactPointSystem.PHONE)
			.setValue(reader.getValue(13, 6) + reader.getValue(13, 7));
		String telUseCode = reader.getValue(13, 2);
		if (!phone.getValue().isBlank()) {
			if (!telUseCode.equals("PRN")) {
				ProcessingException pe = new ProcessingException(
					"Patient phone telecommunication type must be PRN ", "PID", 1, 13);
				if (!processingFlavorSet.contains(ProcessingFlavor.QUINZE)) {
					pe.setErrorCode(IisReportableSeverity.WARN.getCode());
				}
				processingExceptionList.add(pe);
			}
			{ // phone number check
				int countNums = 0;
				boolean invalidCharFound = false;
				char invalidChar = ' ';
				for (char c : phone.getValue().toCharArray()) {
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
					pe.setErrorCode(IisReportableSeverity.WARN.getCode());
					processingExceptionList.add(pe);
				}
				if (countNums != 10 || phone.getValue().startsWith("555") || phone.getValue().startsWith("0")
					|| phone.getValue().startsWith("1")) {
					ProcessingException pe = new ProcessingException(
						"Patient phone number does not appear to be valid", "PID", 1, 13);
					pe.setErrorCode(IisReportableSeverity.WARN.getCode());
					processingExceptionList.add(pe);
				}
			}
		}
		if (!telUseCode.equals("PRN")) {
			phone = null;
		}

		return new Bundle();


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
//			managingOrganization = (Organization) fhirRequester.searchOrganization(Organization.IDENTIFIER.exactly()
//				.systemAndIdentifier(reader.getValue(22,7), managingIdentifier));
			if (managingOrganization == null) {
				managingOrganization = new Organization();
				managingOrganization.setName(reader.getValue(22, 1));
				managingOrganization.addIdentifier()
					.setValue(managingIdentifier)
					.setSystem(reader.getValue(22, 7));
			}
		}
		if (managingOrganization != null) {
//			managingOrganization = (Organization) fhirRequester.saveOrganization(managingOrganization);
		}
		return managingOrganization;

	}
}
