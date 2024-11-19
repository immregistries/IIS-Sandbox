package org.immregistries.iis.kernal.logic.ack;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.mqe.hl7util.builder.AckResult;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Taken from Mqe-util AckBuilder to allow introduction of new codes
 */
public enum IisAckBuilder {
	INSTANCE;
	public static final String PROCESSING_ID_DEBUG = "D";

	public String buildAckFrom(IisAckData ackDataIn) {

		String controlId = ackDataIn.getMessageControlId();
		String processingId = ackDataIn.getProcessingControlId();
		String ackCode;
		String hl7ErrorCode = "0";
		if (hasErrorSeverityType(ackDataIn, IisReportableSeverity.ERROR.getCode())) {
			ackCode = AckResult.APP_ERROR.getCode();
			for (IisReportable r : ackDataIn.getReportables()) {
				if (r.getSeverity() == IisReportableSeverity.ERROR && r.getHl7ErrorCode() != null
					&& r.getHl7ErrorCode().getIdentifier() != null) {
					hl7ErrorCode = r.getHl7ErrorCode().getIdentifier();
					if (hl7ErrorCode != null && hl7ErrorCode.startsWith("2")) {
						ackCode = AckResult.APP_REJECT.getCode();
						break;
					}
				}
			}
		} else if (hasErrorSeverityType(ackDataIn, IisReportableSeverity.WARN.getCode())) {
			ackCode = "AW";
		} else if (hasErrorSeverityType(ackDataIn, "N")) {
			ackCode = "AN";
		} else {
			ackCode = AckResult.APP_ACCEPT.getCode();
		}
		StringBuilder ack = new StringBuilder();
		makeHeader(ack, ackDataIn, StringUtils.defaultIfBlank(ackDataIn.getProfileId(), "Z23"), null);
		// ack.append("SFT|" + SoftwareVersion.VENDOR + "|" +
		// SoftwareVersion.VERSION + "|" + SoftwareVersion.PRODUCT + "|" +
		// SoftwareVersion.BINARY_ID
		// + "|\r");
		ack.append("MSA|" + ackCode + "|" + controlId + "|\r");
		for (IisReportable r : ackDataIn.getReportables()) {
			if (r.getSeverity() == IisReportableSeverity.ERROR) {
				ack.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		for (IisReportable r : ackDataIn.getReportables()) {
			if (r.getSeverity() == IisReportableSeverity.WARN) {
				ack.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		for (IisReportable r : ackDataIn.getReportables()) {
			if (r.getSeverity() == IisReportableSeverity.INFO) {
				ack.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		if (PROCESSING_ID_DEBUG.equals(processingId)) {
			for (IisReportable r : ackDataIn.getReportables()) {
				if (r.getSeverity() == IisReportableSeverity.ACCEPT) {
					ack.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
				}
			}
		}
		return ack.toString();
	}

//  public static void makeERRSegment(StringBuilder ack, String severity, String hl7ErrorCode, String textMessage, Reportable reportable)
//  {
//
//    if (severity.equals("E") && StringUtils.isBlank(hl7ErrorCode))
//    {
//      hl7ErrorCode = "102";
//    }
//    ack.append("ERR||");
//    // 2 Error Location
//    ack.append("|" + reportable.getHl7LocationList());
//    // 3 HL7 Error Code
//    IisHL7Util.appendErrorCode(ack, reportable.getHl7ErrorCode());
//    ack.append("|");
//    // 4 Severity
//    ack.append(severity);
//    ack.append("|");
//    // 5 Application Error Code
//    IisHL7Util.appendAppErrorCode(ack, reportable);
//    ack.append("|");
//    // 6 Application Error Parameter
//    ack.append("|");
//    // 7 Diagnostic Information
//    ack.append("|");
//    // 8 User Message
//    ack.append(IisHL7Util.escapeHL7Chars(reportable.getReportedMessage()));
//    ack.append("|\r");
//
//  }

	private static boolean hasErrorSeverityType(IisAckData ackDataIn, String severityCode) {
		for (IisReportable reportable : ackDataIn.getReportables()) {
			if (reportable.getSeverity().getCode().equals(severityCode)) {
				return true;
			}
		}
		return false;
	}

	public static void makeHeader(StringBuilder ack, IisAckData ackDataIn, String profileId,
											String responseType) {
		String profileExtension = ackDataIn.getProfileExtension();
		String receivingApplication = ackDataIn.getSendingApplication();
		String receivingFacility = ackDataIn.getSendingFacility();
		String sendingApplication = ackDataIn.getReceivingApplication();
		String sendingFacility = ackDataIn.getReceivingFacility();
		if (receivingApplication == null) {
			receivingApplication = "";
		}
		if (receivingFacility == null) {
			receivingFacility = "";
		}
		if (sendingApplication == null || sendingApplication.equals("")) {
			sendingApplication = "MCIR";
		}
		if (sendingFacility == null || sendingFacility.equals("")) {
			sendingFacility = "MCIR";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
		String messageDate = sdf.format(new Date());
		// MSH
		ack.append("MSH|^~\\&");
		ack.append("|" + sendingApplication); // MSH-3 Sending Application
		ack.append("|" + sendingFacility); // MSH-4 Sending Facility
		ack.append("|" + receivingApplication); // MSH-5 Receiving Application
		ack.append("|" + receivingFacility); // MSH-6 Receiving Facility
		ack.append("|" + messageDate); // MSH-7 Date/Time of Message
		ack.append("|"); // MSH-8 Security
		if (responseType == null) {
			responseType = "ACK^V04^ACK";
		}
		ack.append("|" + responseType); // MSH-9
		// Message
		// Type
		ack.append("|" + messageDate + "." + getNextAckCount()); // MSH-10 Message
		// Control ID
		ack.append("|P"); // MSH-11 Processing ID
		ack.append("|2.5.1"); // MSH-12 Version ID
		ack.append("|");
		if (profileId != null) {
			ack.append("||NE|NE|||||").append(profileId).append("^CDCPHINVS");
			if (StringUtils.isNotBlank(profileExtension)) {
				ack.append("~").append(profileExtension).append("^CDCPHINVS");
			}
			ack.append("|");
		}
		ack.append("\r");

	}

	private static int ackCount = 1;

	public static synchronized int getNextAckCount() {
		if (ackCount == Integer.MAX_VALUE) {
			ackCount = 1;
		}
		return ackCount++;
	}

}
