package org.immregistries.iis.kernal.logic;

import org.hibernate.Session;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.Tenant;
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
//	protected Session dataSession;

	public MessageRecordingService() {
//		dataSession = ServletHelper.getDataSession();
	}

	public void recordMessageReceived(String message, PatientMaster patient, String messageResponse, String categoryRequest, String categoryResponse, Tenant tenant) {
		try (Session dataSession = ServletHelper.getDataSession()) {
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
			dataSession.persist(messageReceived);
		}

	}
}
