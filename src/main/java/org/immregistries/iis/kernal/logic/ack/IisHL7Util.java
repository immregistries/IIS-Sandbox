package org.immregistries.iis.kernal.logic.ack;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.builder.AckERRCode;
import org.immregistries.mqe.hl7util.builder.AckResult;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.logic.IIncomingMessageHandler.ADVANCED_ACK;
import static org.immregistries.iis.kernal.logic.ack.IisAckBuilder.PROCESSING_ID_DEBUG;
import static org.immregistries.mqe.vxu.parse.HL7ParsingUtil.escapeHL7Chars;

public class IisHL7Util {

	public static final String MESSAGE_TYPE_VXU = "VXU";
	public static final String MESSAGE_TYPE_QBP = "QBP";

	public static final String ACK_ERROR = "AE";
	public static final String ACK_ACCEPT = "AA";
	public static final String ACK_REJECT = "AR";

	public static final String SEVERITY_ERROR = "E";
	public static final String SEVERITY_WARNING = "W";
	public static final String SEVERITY_INFORMATION = "I";
	public static final String SEVERITY_NOTICE = "N";

	public static final String PROCESSING_ID_DEBUGGING = "D";
	public static final String PROCESSING_ID_PRODUCTION = "P";
	public static final String PROCESSING_ID_TRAINING = "T";

	public static final String QUERY_RESULT_NO_MATCHES = "Z34";
	public static final String QUERY_RESULT_LIST_OF_CANDIDATES = "Z31";
	public static final String QUERY_RESULT_IMMUNIZATION_HISTORY = "Z32";

	public static final String QUERY_RESPONSE_TYPE = "RSP^K11^RSP_K11";

	public static final int BAR = 0;
	public static final int CAR = 1;
	public static final int TIL = 2;
	public static final int SLA = 3;
	public static final int AMP = 4;

	private static int ackCount = 1;

	public static synchronized int getNextAckCount() {
		if (ackCount == Integer.MAX_VALUE) {
			ackCount = 1;
		}
		return ackCount++;
	}

	public static boolean setupSeparators(String messageText, char[] separators) {
		if (messageText.startsWith("MSH") && messageText.length() > 10) {
			separators[BAR] = messageText.charAt(BAR + 3);
			separators[CAR] = messageText.charAt(CAR + 3);
			separators[TIL] = messageText.charAt(TIL + 3);
			separators[SLA] = messageText.charAt(SLA + 3);
			separators[AMP] = messageText.charAt(AMP + 3);
			return true;
		} else {
			setDefault(separators);
			return false;
		}
	}

	public static void setDefault(char[] separators) {
		separators[BAR] = '|';
		separators[CAR] = '^';
		separators[TIL] = '~';
		separators[SLA] = '\\';
		separators[AMP] = '&';
	}

	public static boolean checkSeparatorsAreValid(char[] separators) {
		boolean unique = true;
		// Make sure separators are unique for each other
		for (int i = 0; i < separators.length; i++) {
			for (int j = i + 1; j < separators.length; j++) {
				if (separators[i] == separators[j]) {
					unique = false;
					break;
				}
			}
		}
		return unique;
	}

	public static String makeAckMessage(String ackType, String severityLevel, String message,
													IisAckData ackData, IisReportable reportable) {
		StringBuilder ack = new StringBuilder();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
		String messageDate = sdf.format(new Date());
		// MSH
		ack.append("MSH|^~\\&");
		ack.append("|" + ackData.getSendingApplication()); // MSH-3 Sending
		// Application
		ack.append("|" + ackData.getSendingFacility()); // MSH-4 Sending Facility
		ack.append("|" + ackData.getReceivingApplication()); // MSH-5 Receiving
		// Application
		ack.append("|" + ackData.getReceivingFacility()); // MSH-6 Receiving
		// Facility
		ack.append("|" + messageDate); // MSH-7 Date/Time of Message
		ack.append("|"); // MSH-8 Security
		ack.append("|ACK"); // MSH-9
		// Message
		// Type
		ack.append("|" + messageDate + "." + getNextAckCount()); // MSH-10 Message
		// Control ID
		ack.append("|P"); // MSH-11 Processing ID
		ack.append("|2.5.1"); // MSH-12 Version ID
		ack.append("|"); // MSH-13
		ack.append("|"); // MSH-14
		ack.append("|"); // MSH-15
		ack.append("|NE"); // MSH-16
		ack.append("|NE"); // MSH-17
		ack.append("|"); // MSH-18
		ack.append("|"); // MSH-19
		ack.append("|"); // MSH-20
		ack.append("|Z23^CDCPHINVS"); // MSH-21 TODO chANGE FOR advanced
		ack.append("|\r");
		// ack.append("SFT|" + SoftwareVersion.VENDOR + "|" +
		// SoftwareVersion.VERSION + "|" + SoftwareVersion.PRODUCT + "|" +
		// SoftwareVersion.BINARY_ID
		// + "|\r");
		ack.append("MSA|" + ackType + "|" + ackData.getProcessingControlId() + "|\r");

		ack.append(makeERRSegment(severityLevel, message, reportable));

		return ack.toString();

	}

	public static void appendErrorCode(StringBuilder ack, CodedWithExceptions cwe) {
		if (cwe != null) {
			AckERRCode code = AckERRCode.getFromString(cwe.getIdentifier());
			if (code == null) {
				code = AckERRCode.CODE_0_MESSAGE_ACCEPTED;
			}
			cwe.setNameOfCodingSystem(AckERRCode.TABLE);
			cwe.setText(code.text);
		}
		printCodedWithExceptions(ack, cwe);
	}

	public static String makeERRSegment(IisReportable reportable, boolean debug) {
		StringBuilder err = new StringBuilder();
		CodedWithExceptions hl7ErrorCode = reportable.getHl7ErrorCode();

		err.append("ERR||");
		err.append(printErr3(reportable));
		// 2 Error Location
		err.append("|");
		// 3 HL7 Error Code
		if (hl7ErrorCode == null) {
			hl7ErrorCode = new CodedWithExceptions();
			hl7ErrorCode.setIdentifier("0");
		}
		IisHL7Util.appendErrorCode(err, reportable.getHl7ErrorCode());
		err.append("|");
		// 4 Severity
		IisReportableSeverity level = reportable.getSeverity();
		err.append(level != null ? (level.getCode().equals("A") ? "I" : level.getCode()) : "E");

		err.append("|");
		// 5 Application Error Code
		appendAppErrorCode(err, reportable);
		err.append("|");
		// 6 Application Error Parameter
		err.append("|");
		// 7 Diagnostic Information
		if (debug && reportable.getDiagnosticMessage() != null) {
			err.append(escapeHL7Chars(reportable.getDiagnosticMessage()));
		}
		// 8 User Message
		err.append("|");
		err.append(escapeHL7Chars(reportable.getReportedMessage()));
		if (reportable.getSource() != ReportableSource.MQE) {
			err.append(escapeHL7Chars(" (reported by " + reportable.getSource() + ")"));
		}
		err.append("|\r");
		return err.toString();
	}

	public static void appendAppErrorCode(StringBuilder ack, IisReportable reportable) {
		if (reportable != null) {
			CodedWithExceptions cwe = reportable.getApplicationErrorCode();
			if (cwe != null) {
				if (StringUtils.isNotBlank(cwe.getIdentifier())) {
					if (cwe.getIdentifier().startsWith("MQE")) {
						cwe.setAlternateIdentifier(cwe.getIdentifier());
						cwe.setAlternateText(cwe.getText());
						cwe.setNameOfAlternateCodingSystem("L");
						cwe.setIdentifier("");
						cwe.setText("");
						cwe.setNameOfCodingSystem("");
					}
				}
				if (StringUtils.isNotBlank(cwe.getIdentifier())) {
					if (cwe.getText() == null || cwe.getText().equals("")) {
						cwe.setNameOfCodingSystem("HL70533");
						if (cwe.getIdentifier().equals("1")) {
							cwe.setText("Illogical Date error");
						} else if (cwe.getIdentifier().equals("2")) {
							cwe.setText("Invalid Date");
						} else if (cwe.getIdentifier().equals("3")) {
							cwe.setText("Illogical Value error");
						} else if (cwe.getIdentifier().equals("4")) {
							cwe.setText("Invalid value");
						} else if (cwe.getIdentifier().equals("5")) {
							cwe.setText("Table value not found");
						} else if (cwe.getIdentifier().equals("6")) {
							cwe.setText("Required observation missing");
						} else if (cwe.getIdentifier().equals("7")) {
							cwe.setText("Required data missing");
						} else if (cwe.getIdentifier().equals("8")) {
							cwe.setText("Data was ignored");
						}
					}
				}
			}
			printCodedWithExceptions(ack, cwe);
		}

	}

	private static void printCodedWithExceptions(StringBuilder ack, CodedWithExceptions cwe) {
		if (cwe != null) {
			if (StringUtils.isNotBlank(cwe.getIdentifier())) {
				ack.append(cwe.getIdentifier());
				ack.append("^");
				ack.append(cwe.getText());
				ack.append("^");
				ack.append(cwe.getNameOfCodingSystem());
				if (StringUtils.isNotBlank(cwe.getAlternateIdentifier())) {
					ack.append("^");
				}
			}
			if (StringUtils.isNotBlank(cwe.getAlternateIdentifier())) {
				ack.append(cwe.getAlternateIdentifier());
				ack.append("^");
				ack.append(cwe.getAlternateText());
				ack.append("^");
				ack.append(cwe.getNameOfAlternateCodingSystem());
			}
		}
	}

	private static String printErr3(IisReportable reportable) {
		StringBuilder ack = new StringBuilder();
		boolean repeating = false;
		if (reportable.getHl7LocationList() != null) {
			for (Hl7Location hl7Location : reportable.getHl7LocationList()) {
				if (hl7Location.hasSegmentId()) {
					if (repeating) {
						ack.append("~");
					}
					repeating = true;
					ack.append(hl7Location.getSegmentId());
					ack.append("^");
					if (hl7Location.getSegmentSequence() == 0) {
						ack.append(1);
					} else {
						ack.append(hl7Location.getSegmentSequence());
					}

					if (hl7Location.hasFieldPosition()) {
						ack.append("^");
						ack.append(hl7Location.getFieldPosition());
						ack.append("^");
						if (hl7Location.getFieldRepetition() == 0) {
							ack.append(1);
						} else {
							ack.append(hl7Location.getFieldRepetition());
						}
						if (hl7Location.hasComponentNumber()) {
							ack.append("^");
							ack.append(hl7Location.getComponentNumber());
							if (hl7Location.hasSubComponentNumber()) {
								ack.append("^");
								ack.append(hl7Location.getSubComponentNumber());
							}
						}
					}
				}
			}
		}
		return ack.toString();
	}

	public static String makeERRSegment(String severity, String textMessage, IisReportable reportable) {
		StringBuilder ack = new StringBuilder();
		ack.append("ERR||");
		// 2 Error Location
		ack.append(reportable.getHl7LocationList() != null && reportable.getHl7LocationList().size() > 0
			? reportable.getHl7LocationList().get(0) : "");
		ack.append("|");
		// 3 HL7 Error Code
		IisHL7Util.appendErrorCode(ack, reportable.getHl7ErrorCode());
		ack.append("|");
		// 4 Severity
		ack.append(severity);
		ack.append("|");
		// 5 Application Error Code
		appendAppErrorCode(ack, reportable);
		ack.append("|");
		// 6 Application Error Parameter
		ack.append("|");
		// 7 Diagnostic Information
		ack.append("|");
		// 8 User Message
		ack.append(escapeHL7Chars(textMessage));
		ack.append("|\r");
		return ack.toString();
	}

	public static void makeMsaAndErr(StringBuilder sb, String controlId, String processingId, String profileExtension, List<IisReportable> reportables) {
		String ackCode = getAckCode(profileExtension, reportables);
		sb.append("MSA|").append(ackCode).append("|").append(controlId).append("|\r");
		for (IisReportable r : reportables) {
			if (r.getSeverity() == IisReportableSeverity.ERROR) {
				sb.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		for (IisReportable r : reportables) {
			if (r.getSeverity() == IisReportableSeverity.WARN) {
				sb.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		for (IisReportable r : reportables) {
			if (r.getSeverity() == IisReportableSeverity.INFO) {
				sb.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		for (IisReportable r : reportables) {
			if (r.getSeverity() == IisReportableSeverity.NOTICE) {
				sb.append(IisHL7Util.makeERRSegment(r, PROCESSING_ID_DEBUG.equals(processingId)));
			}
		}
		if (PROCESSING_ID_DEBUG.equals(processingId)) {
			for (IisReportable r : reportables) {
				if (r.getSeverity() == IisReportableSeverity.ACCEPT) {
					sb.append(IisHL7Util.makeERRSegment(r, true));
				}
			}
		}
	}

	private static boolean hasErrorSeverityType(List<IisReportable> reportables, String severityCode) {
		for (IisReportable reportable : reportables) {
			if (reportable.getSeverity().getCode().equals(severityCode)) {
				return true;
			}
		}
		return false;
	}

	private static String getAckCode(String profileExtension, List<IisReportable> reportables) {
		String ackCode;
		String hl7ErrorCode;
		if (ADVANCED_ACK.equals(profileExtension)) {  // If extended ACK profile
			if (hasErrorSeverityType(reportables, IisReportableSeverity.ERROR.getCode())) {
				ackCode = AckResult.APP_ERROR.getCode();
				for (IisReportable r : reportables) {
					if (r.getSeverity() == IisReportableSeverity.ERROR && r.getHl7ErrorCode() != null
						&& r.getHl7ErrorCode().getIdentifier() != null) {
						hl7ErrorCode = r.getHl7ErrorCode().getIdentifier();
						if (hl7ErrorCode != null && hl7ErrorCode.startsWith("2")) {
							ackCode = AckResult.APP_REJECT.getCode();
							break;
						}
					}
				}
			} else if (hasErrorSeverityType(reportables, IisReportableSeverity.WARN.getCode())) {
				ackCode = "AW";
			} else if (hasErrorSeverityType(reportables, "N")) {
				ackCode = "AN";
			} else {
				ackCode = AckResult.APP_ACCEPT.getCode();
			}
		} else {
			List<IisReportable> list = reportables.stream().filter(iisReportable -> iisReportable.getSeverity().equals(IisReportableSeverity.NOTICE)).collect(Collectors.toList());
			reportables = list;
			if (hasErrorSeverityType(reportables, IisReportableSeverity.ERROR.getCode()) || hasErrorSeverityType(reportables, IisReportableSeverity.WARN.getCode())) {
				ackCode = AckResult.APP_ERROR.getCode();
				for (IisReportable r : reportables) {
					if (r.getSeverity() == IisReportableSeverity.ERROR && r.getHl7ErrorCode() != null
						&& r.getHl7ErrorCode().getIdentifier() != null) {
						hl7ErrorCode = r.getHl7ErrorCode().getIdentifier();
						if (hl7ErrorCode != null && hl7ErrorCode.startsWith("2")) {
							ackCode = AckResult.APP_REJECT.getCode();
							break;
						}
					}
				}
			} else {
				ackCode = AckResult.APP_ACCEPT.getCode();
			}
		}
		return ackCode;
	}

}
