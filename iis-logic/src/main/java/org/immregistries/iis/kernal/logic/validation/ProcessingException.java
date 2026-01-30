package org.immregistries.iis.kernal.logic.validation;

import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverityLevel;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;

@SuppressWarnings("serial")
public class ProcessingException extends Exception {

  private String segmentId = "";
  private int segmentRepeat = 0;
  private int fieldPosition = 0;
	private IisReportableSeverityLevel errorCode = IisReportableSeverityLevel.ERROR;
	private CodedWithExceptions applicationErrorCode = new CodedWithExceptions();

	public String getSegmentId() {
    return segmentId;
  }

  public int getSegmentRepeat() {
    return segmentRepeat;
  }

  public int getFieldPosition() {
    return fieldPosition;
  }

  public ProcessingException(String message, String segmentId, int segmentRepeat,
      int fieldPosition) {
    super(message);
    this.segmentId = segmentId;
    this.segmentRepeat = segmentRepeat;
    this.fieldPosition = fieldPosition;
  }

	public ProcessingException(String message, String segmentId, int segmentRepeat,
										int fieldPosition, IisReportableSeverityLevel errorCode) {
		this(message, segmentId, segmentRepeat, fieldPosition);
		this.errorCode = errorCode;
	}

	public ProcessingException(String message, String segmentId, int segmentRepeat,
										int fieldPosition, CodedWithExceptions applicationErrorCode) {
		this(message, segmentId, segmentRepeat, fieldPosition);
		this.applicationErrorCode = applicationErrorCode;
	}

	public ProcessingException(String message, String segmentId, int segmentRepeat,
										int fieldPosition, IisReportableSeverityLevel errorCode, CodedWithExceptions applicationErrorCode) {
		this(message, segmentId, segmentRepeat, fieldPosition);
		this.errorCode = errorCode;
		this.applicationErrorCode = applicationErrorCode;
	}

	private ProcessingException(IisReportable iisReportable) {
		super(iisReportable.getDiagnosticMessage());
		Hl7Location hl7Location = iisReportable.getHl7LocationList().get(0);
		this.segmentId = hl7Location.getSegmentId();
		this.segmentRepeat = hl7Location.getFieldRepetition();
		this.fieldPosition = hl7Location.getFieldPosition();
		this.errorCode = iisReportable.getSeverity();
		this.applicationErrorCode = iisReportable.getApplicationErrorCode();
	}


	public IisReportableSeverityLevel getErrorCode() {
    return errorCode;
  }

	public void setErrorCode(IisReportableSeverityLevel errorCode) {
    this.errorCode = errorCode;
  }

	public static ProcessingException fromIisReportable(IisReportable iisReportable) {
		return new ProcessingException(iisReportable);
	}


	public CodedWithExceptions getApplicationErrorCode() {
		return applicationErrorCode;
	}

	public void setApplicationErrorCode(CodedWithExceptions applicationErrorCode) {
		this.applicationErrorCode = applicationErrorCode;
	}
}
