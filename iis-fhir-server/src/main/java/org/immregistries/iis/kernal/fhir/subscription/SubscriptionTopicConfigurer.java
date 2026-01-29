package org.immregistries.iis.kernal.fhir.subscription;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.hl7.fhir.r5.model.SubscriptionTopic;
import org.immregistries.iis.kernal.flogic.SubscriptionTopicGenerator;

public class SubscriptionTopicConfigurer {

	private DaoRegistry myDaoRegistry;
	private IFhirResourceDao<SubscriptionTopic> mySubscriptionTopicDao;


	public SubscriptionTopicConfigurer(DaoRegistry theDaoRegistry) {
		this.myDaoRegistry = theDaoRegistry;
	}

	@PostConstruct
	public void start() {
		if (mySubscriptionTopicDao == null) {
			mySubscriptionTopicDao = myDaoRegistry.getResourceDao("SubscriptionTopic");
		}
		saveTopic(SubscriptionTopicGenerator.getDataQualityIssuesSubscriptionTopic());
		saveTopic(SubscriptionTopicGenerator.getGroupSubscriptionTopic());
		saveTopic(SubscriptionTopicGenerator.getPatientSubscriptionTopic());
	}

	private void saveTopic(SubscriptionTopic topic) {
		RequestDetails requestDetails = SystemRequestDetails.forAllPartitions();
		try {
			mySubscriptionTopicDao.read(topic.getIdElement(), requestDetails);
		} catch (ResourceNotFoundException | ResourceGoneException e) {
			mySubscriptionTopicDao.update(topic, requestDetails);
		}
	}
}
