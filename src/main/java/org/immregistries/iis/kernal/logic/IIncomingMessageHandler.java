package org.immregistries.iis.kernal.logic;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.smm.tester.manager.HL7Reader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

public interface IIncomingMessageHandler {
	double MINIMAL_MATCHING_SCORE = 0.9;
	int NAME_SIZE_LIMIT = 15;
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

	List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient);

	String buildAck(HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet);

	static SimpleDateFormat generateV2SDF() {
		return new SimpleDateFormat("yyyyMMdd");
	}

	static Date parseDateWarn(String dateString, String errorMessage, String segmentId, int segmentRepeat, int fieldPosition, boolean strict, List<IisReportable> iisReportableList) {
		try {
			return parseDateInternal(dateString, strict);
		} catch (ParseException e) {
			if (errorMessage != null) {
				ProcessingException pe = new ProcessingException(errorMessage + ": " + e.getMessage(), segmentId, segmentRepeat, fieldPosition, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
		}
		return null;
	}

	static Date parseDateInternal(String dateString, boolean strict) throws ParseException {
		if (StringUtils.isBlank(dateString)) {
			return null;
		}
		Date date;
		if (dateString.length() > 8) {
			dateString = dateString.substring(0, 8);
		}
		SimpleDateFormat simpleDateFormat = generateV2SDF();
		simpleDateFormat.setLenient(!strict);
		date = simpleDateFormat.parse(dateString);
		return date;
	}


}
