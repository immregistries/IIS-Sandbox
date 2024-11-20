package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.mqe.hl7util.model.Hl7Location;

@SuppressWarnings("serial")
public class ProcessingException extends Exception {

  private String segmentId = "";
  private int segmentRepeat = 0;
  private int fieldPosition = 0;
	private String errorCode = IisReportableSeverity.ERROR.getCode();

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

	private ProcessingException(IisReportable iisReportable) {
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

	public static ProcessingException fromIisReportable(IisReportable iisReportable) {
		return new ProcessingException(iisReportable);
	}


}
