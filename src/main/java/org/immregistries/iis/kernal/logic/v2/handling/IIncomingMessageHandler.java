package org.immregistries.iis.kernal.logic.v2.handling;

import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;

import java.util.List;
import java.util.Random;
import java.util.Set;

public interface IIncomingMessageHandler<SourceType> {
	int NAME_SIZE_LIMIT = 15;

	String RXA = "RXA";
	String RXR = "RXR";
	String ORC = "ORC";
	String PD_1 = "PD1";
	String PID = "PID";
	String OBX = "OBX";
	String OBR = "OBR";
	String VXU = "VXU";
	String MSH = "MSH";

	String COMPLETION_STATUS_COMPLETE = "CP";
	String COMPLETION_STATUS_PARTIALLY_ADMINISTERED = "PA";
	String INFORMATION_SOURCE_NEW = "00";
	String OBX_CODE_FUNDING_ELIGIBILITY = "64994-7";
	String OBX_CODE_FUNDING_SOURCE = "30963-3";
	String VACCINE_CODE_TYPE_CPT = "CPT";
	String VACCINE_CODE_TYPE_NDC = "NDC";
	String VACCINE_CODE_TYPE_C_4 = "C4";
	String VACCINE_CODE_TYPE_C_5 = "C5";
	String EMAIL_USE_CODE = "NET";
	
	String RSP_K_11_RSP_K_11 = "RSP^K11^RSP_K11";
	String MATCH = "Match";
	String NO_MATCH = "No Match";
	String POSSIBLE_MATCH = "Possible Match";
	String TOO_MANY_MATCHES = "Too Many Matches";

	String PATIENT_MIDDLE_NAME_MULTI = "Multi";
	String QBP_Z34 = "Z34";
	String QBP_Z44 = "Z44";
	String RSP_Z42_MATCH_WITH_FORECAST = "Z42";
	String RSP_Z32_MATCH = "Z32";
	String RSP_Z31_MULTIPLE_MATCH = "Z31";
	String RSP_Z33_NO_MATCH = "Z33";
	String Z23_ACKNOWLEDGEMENT = "Z23";
	String VXU_Z22 = "Z22";
	String ADVANCED_ACK = "ADVANCED_ACK";
	String QUERY_OK = "OK";
	// TODO:
	// Organize logic classes, need to have access classes for every object, maybe a new Access
	// package?
	// Look at names of database fields, make more consistent
	String QUERY_NOT_FOUND = "NF";
	String QUERY_TOO_MANY = "TM";
	String QUERY_APPLICATION_ERROR = "AE";
	Random random = new Random();

	String process(String message, Tenant tenant, String facilityName);

	String buildResultWithoutValidation(SourceType sourceType, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet);

	static void verifyNoErrors(List<IisReportable> iisReportableList) throws ProcessingException {
		for (IisReportable reportable : iisReportableList) {
			if (reportable.getSeverity().equals(IisReportableSeverity.ERROR)) {
				throw ProcessingException.fromIisReportable(reportable);
			}
		}
	}


	static boolean hasErrors(List<IisReportable> reportables) {
		for (IisReportable reportable : reportables) {
			if (reportable.getSeverity().equals(IisReportableSeverity.ERROR)) {
				return true;
			}
		}
		return false;
	}

}
