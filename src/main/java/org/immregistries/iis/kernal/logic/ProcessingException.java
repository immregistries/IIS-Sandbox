package org.immregistries.iis.kernal.logic;

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

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }
}
