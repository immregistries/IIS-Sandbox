package org.immregistries.iis.kernal.logic;

import org.immregistries.mqe.hl7util.Reportable;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.SeverityLevel;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.util.ArrayList;
import java.util.List;

public class NISTReportable implements Reportable {

	private CodedWithExceptions applicationErrorCode = new CodedWithExceptions();
	private String diagnosticMessage = null;
	private CodedWithExceptions hl7ErrorCode = new CodedWithExceptions();
	private List<Hl7Location> hl7LocationList = new ArrayList();
	private String reportedMessage = null;
	private SeverityLevel severity = null;
	private ReportableSource source;

	public NISTReportable() {
		this.source = ReportableSource.NIST;
		this.severity = SeverityLevel.WARN;
	}

	public ReportableSource getSource() {
		return this.source;
	}

	public void setApplicationErrorCode(CodedWithExceptions applicationErrorCode) {
		this.applicationErrorCode = applicationErrorCode;
	}

	public void setDiagnosticMessage(String diagnosticMessage) {
		this.diagnosticMessage = diagnosticMessage;
	}

	public void setHl7ErrorCode(CodedWithExceptions hl7ErrorCode) {
		this.hl7ErrorCode = hl7ErrorCode;
	}

	public void setHl7LocationList(List<Hl7Location> hl7LocationList) {
		this.hl7LocationList = hl7LocationList;
	}

	public void setReportedMessage(String reportedMessage) {
		this.reportedMessage = reportedMessage;
	}

	public CodedWithExceptions getApplicationErrorCode() {
		return this.applicationErrorCode;
	}

	public String getDiagnosticMessage() {
		return this.diagnosticMessage;
	}

	public CodedWithExceptions getHl7ErrorCode() {
		return this.hl7ErrorCode;
	}

	public List<Hl7Location> getHl7LocationList() {
		return this.hl7LocationList;
	}

	public String getReportedMessage() {
		return this.reportedMessage;
	}

	public SeverityLevel getSeverity() {
		return this.severity;
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

}

