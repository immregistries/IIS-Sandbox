package org.immregistries.iis.kernal.logic.ack;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.model.ProcessingFlavor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Taken from Mqe-util AckBuilder to allow introduction of new codes
 */
public enum IisAckBuilder {
	INSTANCE;
	public static final String PROCESSING_ID_DEBUG = "D";

	public String buildAckFrom(IisAckData ackDataIn, Set<ProcessingFlavor> processingFlavorSet) {

		String controlId = ackDataIn.getMessageControlId();
		String processingId = ackDataIn.getProcessingControlId();
		String hl7ErrorCode = "0";

		StringBuilder ack = new StringBuilder();
		makeHeader(ack, ackDataIn, StringUtils.defaultIfBlank(ackDataIn.getProfileId(), "Z23"), null);
		// ack.append("SFT|" + SoftwareVersion.VENDOR + "|" +
		// SoftwareVersion.VERSION + "|" + SoftwareVersion.PRODUCT + "|" +
		// SoftwareVersion.BINARY_ID
		// + "|\r");
		String profileExtension = ackDataIn.getProfileExtension();
		List<IisReportable> reportables = ackDataIn.getReportables();

		IisHL7Util.makeMsaAndErr(ack, controlId, processingId, profileExtension, reportables, processingFlavorSet);
		return ack.toString();
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
		ack.append("|" + responseType); // MSH-9 Message Type
		ack.append("|" + messageDate + "." + getNextAckCount()); // MSH-10 Message Control ID
		ack.append("|T"); // MSH-11 Processing ID TODO figure wether to remove or not
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
