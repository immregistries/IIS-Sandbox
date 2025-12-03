package org.immregistries.iis.kernal.logic;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.persisted.MessageReceived;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
/**
 * Deals with storing records of received Hl7v2, through the hibernate database
 */
public class MessageRecordingService {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public MessageRecordingService() {
	}

	public void recordMessageReceived(String message, PatientMaster patient, String messageResponse,
			String categoryRequest, String categoryResponse, Tenant tenant) {
		MessageReceived messageReceived = getMessageReceived(message, patient, messageResponse, categoryRequest,
				categoryResponse, tenant);
		recordMessageReceived(messageReceived);
	}

	private void recordMessageReceived(MessageReceived messageReceived) {
		try (Session dataSession = HibernateConfig.getDataSession()) {
			Transaction transaction = dataSession.beginTransaction();
			dataSession.persist(messageReceived);
			transaction.commit();
		}
	}

	private static @NotNull MessageReceived getMessageReceived(String message, PatientMaster patient,
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
