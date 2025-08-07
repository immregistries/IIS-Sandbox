package org.immregistries.iis.kernal.logic.ack;

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
		hl7LocationList = List.copyOf(reportable.getHl7LocationList());
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


	public boolean isError() {
		return IisReportableSeverity.ERROR.equals(this.severity);
	}

	public boolean isWarning() {
		return IisReportableSeverity.WARN.equals(this.severity);
	}

}
