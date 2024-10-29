package org.immregistries.iis.kernal.logic;

import org.immregistries.mqe.hl7util.Reportable;
import org.immregistries.mqe.hl7util.ReportableSource;
import org.immregistries.mqe.hl7util.SeverityLevel;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

import java.util.List;

public class ProcessingExceptionReportable implements Reportable {
	ProcessingException processingException;

	public ProcessingExceptionReportable(ProcessingException processingException) {
		this.processingException = processingException;
	}

	@Override
	public SeverityLevel getSeverity() {
		return SeverityLevel.findByCode(processingException.getErrorCode());
	}

	@Override
	public CodedWithExceptions getHl7ErrorCode() {
		CodedWithExceptions codedWithExceptions = new CodedWithExceptions();
		return new CodedWithExceptions();
	}

	@Override
	public List<Hl7Location> getHl7LocationList() {
		Hl7Location location = new Hl7Location();
		location.setSegmentId(processingException.getSegmentId());
		location.setFieldRepetition(processingException.getSegmentRepeat());
		location.setFieldPosition(processingException.getFieldPosition());
		return List.of(location);
	}

	@Override
	public String getReportedMessage() {
		return processingException.getLocalizedMessage();
	}

	@Override
	public String getDiagnosticMessage() {
		return processingException.getMessage();
	}

	@Override
	public CodedWithExceptions getApplicationErrorCode() {
		CodedWithExceptions codedWithExceptions = new CodedWithExceptions();
		codedWithExceptions.setIdentifier("101");
		codedWithExceptions.setNameOfCodingSystem("HL70357TEST");
		codedWithExceptions.setText("Required field missing");
		return codedWithExceptions;
	}

	@Override
	public ReportableSource getSource() {
		return ReportableSource.IIS;
	}
}
