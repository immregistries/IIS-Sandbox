package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.logic.recommendations.IImmunizationRecommendationService;
import org.immregistries.iis.kernal.logic.recommendations.IisRecommendationGenerator;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

import static org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE;

@RestController
@RequestMapping(RestUrlUtil.REST_TENANT_PATH + RecommendationRestController.RECOMMENDATION_PATH)
public class RecommendationRestController {

	public static final String RECOMMENDATION_PATH = "/recommendation";
	public static final String RECOMMENDATION_ID = "recommendationId";
	public static final String RECOMMENDATION_IDENTIFIER = "recommendationIdentifier";
	@Autowired
    private IImmunizationRecommendationService immunizationRecommendationService;
    @Autowired
    private IisFhirClientFactory iisFhirClientFactory;
    @Autowired
	 private FhirSearchRequester fhirSearchRequester;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private PatientMapper patientMapper;
	 @Autowired
	 private IisRecommendationGenerator iisRecommendationGenerator;

    @PostMapping("/random")
    public void addRandomRecommendation(
            @RequestAttribute(name = TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {

        IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);
        IDomainResource patient = PatientServletUtil.fetchPatientFromParameter(req, fhirClient, fhirSearchRequester);
		 IisPatient patientMaster = patientMapper.localObject(patient);

        if (patient != null) {
			  IAnyResource recommendation = immunizationRecommendationService.getPatientRecommendation(fhirClient, patient);
			  if (recommendation != null) {
				  recommendation = iisRecommendationGenerator.addRandomGeneratedRecommendation(recommendation);
				  immunizationRecommendationService.updateRecommendation(fhirClient, recommendation);
			  } else {
				  fhirClient.create()
					  .resource(iisRecommendationGenerator.generateFhirRecommendation(tenant, new Date(), patientMaster))
					  .execute();
			  }
        }
    }


	@PutMapping
    public void updateRecommendation(
            @RequestAttribute(TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestBody String recommendationResource,
				@RequestParam(name = RECOMMENDATION_ID, required = false) String recommendationId,
				@RequestParam(name = RECOMMENDATION_IDENTIFIER, required = false) String recommendationIdentifier,
            HttpServletRequest req) {
        IParser parser = iisFhirClientFactory.getFhirContext()
                .newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);
        IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);
		IAnyResource old = getRecommendation(recommendationId, recommendationIdentifier, tenant, req);
		IAnyResource newRecommendation = parser.parseResource(IAnyResource.class, recommendationResource);
		if (old != null) {
			newRecommendation.setId(old.getIdElement().getIdPart());
		}
		fhirClient.update().resource(newRecommendation).execute();
    }

    @GetMapping()
	 public IAnyResource getRecommendation(
		 @RequestParam(name = RECOMMENDATION_ID, required = false) String recommendationId,
		 @RequestParam(name = RECOMMENDATION_IDENTIFIER, required = false) String recommendationIdentifier,
            @RequestAttribute(TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
		 return immunizationRecommendationService.readRecommendation(recommendationId, recommendationIdentifier, fhirClient);
	 }

}
