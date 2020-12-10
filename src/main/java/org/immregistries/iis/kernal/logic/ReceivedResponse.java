package org.immregistries.iis.kernal.logic;

import java.util.Date;

public class ReceivedResponse {
  private String receivedMessage = "";
  private String responseMessage = "";
  private Date receivedDate = new Date();

  public Date getReceivedDate() {
    return receivedDate;
  }

  public String getReceivedMessage() {
    return receivedMessage;
  }

  protected void setReceivedMessage(String received) {
    this.receivedMessage = received;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  protected void setResponseMessage(String response) {
    this.responseMessage = response;
  }
}
