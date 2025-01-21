package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.forR4.PatientMapperR4;
import org.immregistries.iis.kernal.mapping.forR5.PatientMapperR5;
import org.immregistries.iis.kernal.model.PatientName;
import org.immregistries.iis.kernal.model.PatientPhone;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.logic.IncomingMessageHandler.NAME_SIZE_LIMIT;

@Interceptor
@Service
public class PatientProcessingInterceptor {
	public String IIS_REPORTABLE_LIST = "iisReportableList";
	@Autowired
	PatientMapperR4 patientMapperR4;
	@Autowired
	PatientMapperR5 patientMapperR5;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2000)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = (List<IisReportable>) requestDetails.getAttribute(IIS_REPORTABLE_LIST);
		PatientName legalName = null;
		IBaseResource result = requestDetails.getResource();
		if (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Patient) {
			PatientReported patientReported = processPatient(patientMapperR4.getReported((org.hl7.fhir.r4.model.Patient) requestDetails.getResource()), processingFlavorSet, iisReportableList);
			result = patientMapperR5.getFhirResource(patientReported);
		} else if (requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Patient) {
			PatientReported patientReported = processPatient(patientMapperR5.getReported((org.hl7.fhir.r5.model.Patient) requestDetails.getResource()), processingFlavorSet, iisReportableList);
			result = patientMapperR5.getFhirResource(patientReported);
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
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}

	private PatientReported processPatient(PatientReported patientReported, Set<ProcessingFlavor> processingFlavorSet, List<IisReportable> iisReportableList) throws ProcessingException {
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


}