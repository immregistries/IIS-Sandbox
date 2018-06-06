package org.immregistries.iis.kernal.logic;

@SuppressWarnings("serial")
public class ProcessingException extends Exception {
  private String segmentId = "";
  private int segmentRepeat = 0;
  private int fieldPosition = 0;
  
  public String getSegmentId() {
    return segmentId;
  }

  public int getSegmentRepeat() {
    return segmentRepeat;
  }

  public int getFieldPosition() {
    return fieldPosition;
  }

  public ProcessingException(String message, String segmentId, int segmentRepeat, int fieldPosition)
  {
    super(message);
    this.segmentId = segmentId;
    this.segmentRepeat = segmentRepeat;
    this.fieldPosition = fieldPosition;
  }
}
