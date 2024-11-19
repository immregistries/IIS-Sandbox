package org.immregistries.iis.kernal.logic.ack;

import org.immregistries.mqe.hl7util.builder.AckResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IisAckData {

	private String messageProfileId = "";
	private String messageVersionId = "";
	private AckResult result = AckResult.APP_ERROR;// default is error???
	private List<IisReportable> reportables = new ArrayList<IisReportable>();
	private String messageControlId = "";
	private String processingControlId = "";
	private String receivingApplication = "";
	private String receivingFacility = "";
	private String sendingApplication = "";
	private String sendingFacility = "";
	private Date messageDate = new Date();
	private String responseType = "";
	private String profileId = "";
	private String profileExtension = "";

	public IisAckData() {
	}

	public IisAckData(String messageProfileId, String messageVersionId,
							AckResult result, List<IisReportable> list, String messageControlId,
							String processingControlId, String receivingApplication,
							String receivingFacility, String sendingApplication,
							String sendingFacility, Date messageDate, String responseType,
							String profileId) {
		this.messageProfileId = messageProfileId;
		this.messageVersionId = messageVersionId;
		this.result = result;
		this.reportables.addAll(list);
		this.messageControlId = messageControlId;
		this.processingControlId = processingControlId;
		this.receivingApplication = receivingApplication;
		this.receivingFacility = receivingFacility;
		this.sendingApplication = sendingApplication;
		this.sendingFacility = sendingFacility;
	}

	public String getMessageProfileId() {
		return messageProfileId;
	}

	public void setMessageProfileId(String messageProfileId) {
		this.messageProfileId = messageProfileId;
	}

	public String getMessageVersionId() {
		return messageVersionId;
	}

	public void setMessageVersionId(String messageVersionId) {
		this.messageVersionId = messageVersionId;
	}

	public AckResult getResult() {
		return result;
	}

	public void setResult(AckResult result) {
		this.result = result;
	}

	public List<IisReportable> getReportables() {
		return reportables;
	}

	public void setReportables(List<IisReportable> reportables) {
		this.reportables = reportables;
	}

	public String getMessageControlId() {
		return messageControlId;
	}

	public void setMessageControlId(String messageControlId) {
		this.messageControlId = messageControlId;
	}

	public String getProcessingControlId() {
		return processingControlId;
	}

	public void setProcessingControlId(String processingControlId) {
		this.processingControlId = processingControlId;
	}

	public String getReceivingApplication() {
		return receivingApplication;
	}

	public void setReceivingApplication(String receivingApplication) {
		this.receivingApplication = receivingApplication;
	}

	public String getReceivingFacility() {
		return receivingFacility;
	}

	public void setReceivingFacility(String receivingFacility) {
		this.receivingFacility = receivingFacility;
	}

	public String getSendingApplication() {
		return sendingApplication;
	}

	public void setSendingApplication(String sendingApplication) {
		this.sendingApplication = sendingApplication;
	}

	public String getSendingFacility() {
		return sendingFacility;
	}

	public void setSendingFacility(String sendingFacility) {
		this.sendingFacility = sendingFacility;
	}

	public Date getMessageDate() {
		return messageDate;
	}

	public void setMessageDate(Date messageDate) {
		this.messageDate = messageDate;
	}

	public String getResponseType() {
		return responseType;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getProfileExtension() {
		return profileExtension;
	}

	public void setProfileExtension(String profileExtension) {
		this.profileExtension = profileExtension;
	}
}
