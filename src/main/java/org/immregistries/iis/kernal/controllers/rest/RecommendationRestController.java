package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.immregistries.iis.kernal.controllers.servlet.util.PatientServletUtil;
import org.immregistries.iis.kernal.logic.recommendations.IImmunizationRecommendationService;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
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

    @PostMapping("/random")
    public void addRandomRecommendation(
            @RequestAttribute(name = TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {

        IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);
        IDomainResource patient = PatientServletUtil.fetchPatientFromParameter(req, fhirClient, fhirSearchRequester);
		 IisPatient patientMaster = patientMapper.localObject(patient);

        if (patient != null) {
            IBaseBundle baseBundle = fhirClient.search().forResource("ImmunizationRecommendation")
                    .where(org.hl7.fhir.r5.model.ImmunizationRecommendation.PATIENT
                            .hasId(new org.hl7.fhir.r5.model.IdType(patient.getId()).getIdPart()))
                    .execute();

            if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
                org.hl7.fhir.r5.model.Bundle recommendationBundle = (org.hl7.fhir.r5.model.Bundle) baseBundle;
                if (recommendationBundle.hasEntry()) {
                    org.hl7.fhir.r5.model.ImmunizationRecommendation recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationBundle
                            .getEntryFirstRep().getResource();
                    recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) immunizationRecommendationService
                            .addRandomGeneratedRecommendation(recommendation);
                    fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
                } else {
                    fhirClient.create()
                            .resource(immunizationRecommendationService.generate(tenant, new Date(), patientMaster))
                            .execute();
                }
            } else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
                org.hl7.fhir.r4.model.Bundle recommendationBundle = (org.hl7.fhir.r4.model.Bundle) baseBundle;
                if (recommendationBundle.hasEntry()) {
                    org.hl7.fhir.r4.model.ImmunizationRecommendation recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle
                            .getEntryFirstRep().getResource();
                    recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) immunizationRecommendationService
                            .addRandomGeneratedRecommendation(recommendation);
                    fhirClient.update().resource(recommendation).withId(recommendation.getId()).execute();
                } else {
                    fhirClient.create()
                            .resource(immunizationRecommendationService.generate(tenant, new Date(), patientMaster))
                            .execute();
                }
            }
        }
    }

    @PutMapping
    public void updateRecommendation(
            @RequestAttribute(TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestBody String recommendationResource,
            HttpServletRequest req) {

        IParser parser = iisFhirClientFactory.getFhirContext()
                .newJsonParser().setPrettyPrint(true).setSummaryMode(false).setSuppressNarratives(true);

        IGenericClient fhirClient = iisFhirClientFactory.getOrCreateGenericClient(req);

        if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
            org.hl7.fhir.r5.model.ImmunizationRecommendation newRecommendation = parser
                    .parseResource(org.hl7.fhir.r5.model.ImmunizationRecommendation.class, recommendationResource);
            // We need to get the ID from the existing one or the payload.
            // The original code fetched 'old' to get the ID.
            // But here we might expect the ID to be in the payload or passed separately.
            // Replicating original logic which seems to rely on finding the recommendation
            // first?
            // Actually the original code calls getRecommendation(req, fhirClient) which
            // looks up by ID or Identifier param.
            // Since this is a REST endpoint, we should probably pass the ID in the URL or
            // body.
            // For strict equivalence, we'll assume the ID is in the body or we look it up
            // same as before if params are present.
            // However, `getRecommendation` relies on request parameters.
            // Let's assume for now the caller passes the ID in the resource or we use the
            // logic if we can.
            // But `getRecommendation` is in the Controller.

            // To keep it simple and equivalent, I'll adapt the logic to expect the ID in
            // the resource or use a helper if needed.
            // But wait, the original code:
            // org.hl7.fhir.r5.model.ImmunizationRecommendation old =
            // (org.hl7.fhir.r5.model.ImmunizationRecommendation) getRecommendation(req,
            // fhirClient);
            // newRecommendation.setId(old.getIdElement().getIdPart());

            // I will duplicate `getRecommendation` logic here or make it static utility?
            // `getRecommendation` uses `PARAM_RECOMMENDATION_ID` or
            // `PARAM_RECOMMENDATION_IDENTIFIER`.
            // I'll assume the client passes these as query params to this PUT endpoint as
            // well, or I'll simplify to just update what's given.
            // If I simplify, I might break the exact behavior.
            // Let's try to support the query params for lookup if provided.

            IDomainResource old = getRecommendation(req.getParameter("recommendationId"),
                    req.getParameter("recommendationIdentifier"), tenant, req);
            if (old != null) {
                newRecommendation.setId(old.getIdElement().getIdPart());
            }
            fhirClient.update().resource(newRecommendation).execute();
        } else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
            org.hl7.fhir.r4.model.ImmunizationRecommendation newRecommendation = parser
                    .parseResource(org.hl7.fhir.r4.model.ImmunizationRecommendation.class, recommendationResource);
            IDomainResource old = getRecommendation(req.getParameter("recommendationId"),
                    req.getParameter("recommendationIdentifier"), tenant, req);
            if (old != null) {
                newRecommendation.setId(old.getIdElement().getIdPart());
            }
            fhirClient.update().resource(newRecommendation).execute();
        }
    }

    @GetMapping()
    public IDomainResource getRecommendation(
            @RequestParam(name = "recommendationId", required = false) String recommendationId,
            @RequestParam(name = "recommendationIdentifier", required = false) String recommendationIdentifier,
            @RequestAttribute(TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IDomainResource recommendation = null;

        IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
        if (recommendationId != null) {
            recommendation = (IDomainResource) fhirClient.read().resource("ImmunizationRecommendation")
                    .withId(recommendationId).execute();
        } else if (recommendationIdentifier != null) {
            if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R5)) {
                org.hl7.fhir.r5.model.Bundle recommendationBundle = fhirClient.search()
                        .forResource("ImmunizationRecommendation").where(
                                org.hl7.fhir.r5.model.Patient.IDENTIFIER.exactly().identifier(recommendationIdentifier))
                        .returnBundle(org.hl7.fhir.r5.model.Bundle.class).execute();
                if (recommendationBundle.hasEntry()) {
                    recommendation = (org.hl7.fhir.r5.model.ImmunizationRecommendation) recommendationBundle
                            .getEntryFirstRep().getResource();
                }
            } else if (fhirContext.getVersion().getVersion().equals(FhirVersionEnum.R4)) {
                org.hl7.fhir.r4.model.Bundle recommendationBundle = fhirClient.search()
                        .forResource("ImmunizationRecommendation").where(
                                org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly().identifier(recommendationIdentifier))
                        .returnBundle(org.hl7.fhir.r4.model.Bundle.class).execute();
                if (recommendationBundle.hasEntry()) {
                    recommendation = (org.hl7.fhir.r4.model.ImmunizationRecommendation) recommendationBundle
                            .getEntryFirstRep().getResource();
                }
            }
        }
        return recommendation;
    }
}
