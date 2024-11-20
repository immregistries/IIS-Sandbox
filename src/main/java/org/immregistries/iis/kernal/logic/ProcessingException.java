package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.mqe.hl7util.model.Hl7Location;

@SuppressWarnings("serial")
public class ProcessingException extends Exception {
  private static final String INFORMATION = "I";
  private static final String WARNING = "W";
  private static final String ERROR = "E";

  private String segmentId = "";
  private int segmentRepeat = 0;
  private int fieldPosition = 0;
  private String errorCode = ERROR;

  public boolean isError() {
    return errorCode.equals(ERROR);
  }

  public boolean isWarning() {
    return errorCode.equals(WARNING);
  }

  public boolean isInformation() {
    return errorCode.equals(INFORMATION);
  }

  public ProcessingException setWarning() {
    errorCode = WARNING;
    return this;
  }

  public ProcessingException setInformation() {
    errorCode = INFORMATION;
    return this;
  }

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
										int fieldPosition, String errorCode) {
		super(message);
		this.segmentId = segmentId;
		this.segmentRepeat = segmentRepeat;
		this.fieldPosition = fieldPosition;
		this.errorCode = errorCode;
	}

	public ProcessingException(IisReportable iisReportable) {
		super(iisReportable.getDiagnosticMessage());
		Hl7Location hl7Location = iisReportable.getHl7LocationList().get(0);
		this.segmentId = hl7Location.getSegmentId();
		this.segmentRepeat = hl7Location.getFieldRepetition();
		this.fieldPosition = hl7Location.getFieldPosition();
		this.errorCode = iisReportable.getSeverity().getCode();
	}


	public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }


}
