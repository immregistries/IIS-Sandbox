package org.immregistries.iis.kernal.services;

import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.persisted.entities.MessageReceived;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.repository.MessageReceivedRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
/**
 * Deals with storing records of received Hl7v2, through the hibernate database
 */
public class MessageRecordingService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MessageReceivedRepository messageReceivedRepository;

	public MessageReceived recordMessageReceived(String message, IisPatient patient, String messageResponse,
																String categoryRequest, String categoryResponse, Tenant tenant) {
		MessageReceived messageReceived = getMessageReceived(message, patient, messageResponse, categoryRequest,
				categoryResponse, tenant);
		return messageReceivedRepository.save(messageReceived);
	}

	private static @NotNull MessageReceived getMessageReceived(String message, IisPatient patient,
			String messageResponse, String categoryRequest, String categoryResponse, Tenant tenant) {
		MessageReceived messageReceived = new MessageReceived();
		messageReceived.setTenant(tenant);
		messageReceived.setMessageRequest(message);
		if (patient != null) {
			messageReceived.setPatientReportedId(patient.getPatientId());
		}
		messageReceived.setMessageResponse(messageResponse);
		messageReceived.setReportedDate(new Date());
		messageReceived.setCategoryRequest(categoryRequest);
		messageReceived.setCategoryResponse(categoryResponse);
		// TODO interact with internal logs and metadata
		return messageReceived;
	}
}
