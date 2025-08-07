package org.immregistries.iis.kernal.logic.ack;

import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.util.List;

public final class ReportableUtil {

	public static IisReportable fromProcessingException(ProcessingException processingException) {
		IisReportable iisReportable = new IisReportable();

		Hl7Location location = new Hl7Location();
		location.setSegmentId(processingException.getSegmentId());
//		location.setComponentNumber(processingException.get);
		location.setFieldRepetition(processingException.getSegmentRepeat());
		location.setFieldPosition(processingException.getFieldPosition());
		iisReportable.setHl7LocationList(List.of(location));

		CodedWithExceptions hl7ErrorCode = new CodedWithExceptions();
		hl7ErrorCode.setIdentifier("101");
		hl7ErrorCode.setNameOfCodingSystem("HL70357");
		hl7ErrorCode.setText("Required field missing");
		iisReportable.setHl7ErrorCode(hl7ErrorCode);

		iisReportable.setSeverity(processingException.getErrorCode());
		iisReportable.setApplicationErrorCode(new CodedWithExceptions());
		iisReportable.setReportedMessage(processingException.getLocalizedMessage());
		iisReportable.setDiagnosticMessage(processingException.getMessage());
		iisReportable.setSource(ReportableSource.IIS);

		return iisReportable;
	}

	/**
	 * Taken from NIST Validator Connector Project and fixed issues
	 *
	 * @param path
	 * @param segmentid
	 * @return
	 */
	public static Hl7Location readErrorLocation(String path, String segmentid) {
		Hl7Location errorLocation = new Hl7Location();
		errorLocation.setSegmentId(segmentid);
		int firstDotPos = path.indexOf("-");
		String segmentSequence = path;
		if (firstDotPos >= 0) {
			segmentSequence = path.substring(0, firstDotPos);
			path = path.substring(firstDotPos + 1);
		} else {
			path = "";
		}

		int sequence = parseBracketInt(segmentSequence);
		if (sequence > 0) {
			errorLocation.setSegmentSequence(sequence);
		}

		if (path.length() > 0) {
			String componentString = path;
			int dotPos = path.indexOf(".");
			if (dotPos >= 0) {
				componentString = path.substring(0, dotPos);
				path = path.substring(dotPos + 1);
			} else {
				path = "";
			}

			int fieldPosition = 0;
			int bracketPos = componentString.indexOf("[");

			try {
				if (bracketPos >= 0) {
					fieldPosition = Integer.parseInt(componentString.substring(0, bracketPos).trim());
					componentString = componentString.substring(bracketPos);
					errorLocation.setFieldRepetition(parseBracketInt(componentString));
				} else {
					fieldPosition = Integer.parseInt(componentString.trim());
				}
			} catch (NumberFormatException var14) {
			}

			if (fieldPosition != 0) {
				errorLocation.setFieldPosition(fieldPosition);
			}

			if (path.length() > 0) {
				componentString = path;
				dotPos = path.indexOf(".");
				if (dotPos >= 0) {
					componentString = path.substring(0, dotPos);
					path = path.substring(dotPos + 1);
				} else {
					path = "";
				}

				try {
					errorLocation.setComponentNumber(Integer.parseInt(componentString.trim()));
				} catch (NumberFormatException var13) {
				}
			}

			if (path.length() > 0) {
				try {
					errorLocation.setSubComponentNumber(Integer.parseInt(path.trim()));
				} catch (NumberFormatException var12) {
				}
			}
		}

		return errorLocation;
	}

	private static int parseBracketInt(String s) {
		if (s.startsWith("[") && s.endsWith("]")) {
			try {
				return Integer.parseInt(s.substring(1, s.length() - 1).trim());
			} catch (NumberFormatException var3) {
			}
		} else {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException var3) {
			}
		}
		return 0;
	}

}
