package org.immregistries.iis.kernal.controllers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.extractor.ISearchParamExtractor;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.servlet.ServletException;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.logic.SubscriptionTopicGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping({"/SubscriptionTopic", IisRestPath.BasePath.REST_PATH + "/SubscriptionTopic"})
public class SubscriptionTopicController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private ISearchParamRegistry iSearchParamRegistry;
	@Autowired
	private ISearchParamExtractor iSearchParamExtractor;

	@GetMapping
	protected SubscriptionTopic doGetDefault() throws ServletException, IOException {
		return SubscriptionTopicGenerator.getDataQualityIssuesSubscriptionTopic();
	}

	@GetMapping(PATIENT_TOPIC_NAME)
	protected SubscriptionTopic doGetPatientTest() throws ServletException, IOException {
		return SubscriptionTopicGenerator.getPatientSubscriptionTopic();
	}

	@GetMapping(GROUP_TOPIC_NAME)
	protected SubscriptionTopic doGetGroup() throws ServletException, IOException {
		return SubscriptionTopicGenerator.getGroupSubscriptionTopic();
	}


	@GetMapping(DATA_QUALITY_ISSUES_TOPIC_NAME)
	protected SubscriptionTopic doGetDataQualityIssues() throws ServletException, IOException {
		return SubscriptionTopicGenerator.getDataQualityIssuesSubscriptionTopic();
	}

}
