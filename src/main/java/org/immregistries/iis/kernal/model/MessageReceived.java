package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.Date;

public class MessageReceived implements Serializable {
  private int messageReceivedId = 0;
  private OrgMaster orgMaster = null;
  private String messageRequest = "";
  private String messageResponse = "";
  private PatientReported patientReported = null;
  private Date reportedDate = null;
  private String categoryRequest = "";
  private String categoryResponse = "";

  public String getCategoryRequest() {
    return categoryRequest;
  }

  public void setCategoryRequest(String categoryRequest) {
    this.categoryRequest = categoryRequest;
  }

  public String getCategoryResponse() {
    return categoryResponse;
  }

  public void setCategoryResponse(String categoryResponse) {
    this.categoryResponse = categoryResponse;
  }

  public int getMessageReceivedId() {
    return messageReceivedId;
  }

  public void setMessageReceivedId(int messageReceivedId) {
    this.messageReceivedId = messageReceivedId;
  }

  public String getMessageRequest() {
    return messageRequest;
  }

  public void setMessageRequest(String messageRequest) {
    this.messageRequest = messageRequest;
  }

  public String getMessageResponse() {
    return messageResponse;
  }

  public void setMessageResponse(String messageResponse) {
    this.messageResponse = messageResponse;
  }

  public PatientReported getPatientReported() {
    return patientReported;
  }

  public void setPatientReported(PatientReported patientReported) {
    this.patientReported = patientReported;
  }

  public Date getReportedDate() {
    return reportedDate;
  }

  public void setReportedDate(Date reportedDate) {
    this.reportedDate = reportedDate;
  }

  public OrgMaster getOrgMaster() {
    return orgMaster;
  }

  public void setOrgMaster(OrgMaster orgMaster) {
    this.orgMaster = orgMaster;
  }
}
