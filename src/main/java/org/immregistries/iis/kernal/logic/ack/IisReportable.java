package org.immregistries.iis.kernal.logic.ack;

import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.mqe.hl7util.Reportable;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.util.ArrayList;
import java.util.List;

public class IisReportable {

	private CodedWithExceptions applicationErrorCode = new CodedWithExceptions();
	private String diagnosticMessage = null;
	private CodedWithExceptions hl7ErrorCode = new CodedWithExceptions();
	private List<Hl7Location> hl7LocationList = new ArrayList();
	private String reportedMessage = null;
	private IisReportableSeverity severity = null;
	private ReportableSource source;

	public IisReportable() {
	}

	public IisReportable(Reportable reportable) {
		applicationErrorCode = reportable.getApplicationErrorCode();
		diagnosticMessage = reportable.getDiagnosticMessage();
		hl7ErrorCode = reportable.getHl7ErrorCode();
		reportedMessage = reportable.getReportedMessage();
		severity = IisReportableSeverity.findByCode(reportable.getSeverity().getCode());
		source = reportable.getSource();
	}

//	public IisReportable(String message, String segmentId, int segmentRepeat,
//										int fieldPosition, String errorCode) {
//		diagnosticMessage = message;
//		Hl7Location location = new Hl7Location();
//		location.setSegmentId(segmentId);
//		location.setFieldRepetition(segmentRepeat);
//		location.setFieldPosition(fieldPosition);
//		hl7LocationList = List.of(location);
//		severity = IisReportableSeverity.findByCode(errorCode);
//		hl7ErrorCode = new CodedWithExceptions();
//		hl7ErrorCode.setIdentifier("101");
//		hl7ErrorCode.setNameOfCodingSystem("HL70357");
//		hl7ErrorCode.setText("Required field missing");
//		applicationErrorCode = new CodedWithExceptions();
//		source = ReportableSource.IIS;
//	}

	private IisReportable(ProcessingException processingException) {
		Hl7Location location = new Hl7Location();
		location.setSegmentId(processingException.getSegmentId());
		location.setFieldRepetition(processingException.getSegmentRepeat());
		location.setFieldPosition(processingException.getFieldPosition());
		hl7LocationList = List.of(location);
		hl7ErrorCode = new CodedWithExceptions();
		hl7ErrorCode.setIdentifier("101");
		hl7ErrorCode.setNameOfCodingSystem("HL70357");
		hl7ErrorCode.setText("Required field missing");
		severity = IisReportableSeverity.findByCode(processingException.getErrorCode());
		applicationErrorCode = new CodedWithExceptions();
		reportedMessage = processingException.getLocalizedMessage();
		diagnosticMessage = processingException.getMessage();
		source = ReportableSource.IIS;
	}

	public CodedWithExceptions getApplicationErrorCode() {
		return applicationErrorCode;
	}

	public void setApplicationErrorCode(CodedWithExceptions applicationErrorCode) {
		this.applicationErrorCode = applicationErrorCode;
	}

	public String getDiagnosticMessage() {
		return diagnosticMessage;
	}

	public void setDiagnosticMessage(String diagnosticMessage) {
		this.diagnosticMessage = diagnosticMessage;
	}

	public CodedWithExceptions getHl7ErrorCode() {
		return hl7ErrorCode;
	}

	public void setHl7ErrorCode(CodedWithExceptions hl7ErrorCode) {
		this.hl7ErrorCode = hl7ErrorCode;
	}

	public List<Hl7Location> getHl7LocationList() {
		return hl7LocationList;
	}

	public void setHl7LocationList(List<Hl7Location> hl7LocationList) {
		this.hl7LocationList = hl7LocationList;
	}

	public String getReportedMessage() {
		return reportedMessage;
	}

	public void setReportedMessage(String reportedMessage) {
		this.reportedMessage = reportedMessage;
	}

	public IisReportableSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(IisReportableSeverity severity) {
		this.severity = severity;
	}

	public ReportableSource getSource() {
		return source;
	}

	public void setSource(ReportableSource source) {
		this.source = source;
	}

	public static IisReportable fromProcessingException(ProcessingException processingException) {
		return new IisReportable(processingException);
	}

	public static Hl7Location readErrorLocation(String path, String segmentid) {
		Hl7Location errorLocation = new Hl7Location();
		errorLocation.setSegmentId(segmentid);
		int firstDotPos = path.indexOf(".");
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
					componentString = componentString.substring(bracketPos + 1);
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
		s = s.trim();
		if (s.startsWith("[") && s.startsWith("]")) {
			try {
				return Integer.parseInt(s.substring(1, s.length() - 1).trim());
			} catch (NumberFormatException var3) {
			}
		}

		return 0;
	}

	public boolean isError() {
		return IisReportableSeverity.ERROR.equals(this.severity);
	}

	public boolean isWarning() {
		return IisReportableSeverity.WARN.equals(this.severity);
	}

}
