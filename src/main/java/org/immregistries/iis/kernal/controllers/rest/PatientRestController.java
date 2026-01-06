package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.controllers.rest.shlink.PatientShLinkRestController;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/patientMaster")
public class PatientRestController extends BaseTenantTiedRest {

    public static final String MDM_EXPAND_REST_PARAM = "isGolden";
    @Autowired
    private RepositoryClientFactory repositoryClientFactory;

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private PatientMapper patientMapper;

    @GetMapping("/{patientId}")
    public PatientMaster getPatient(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
        return fhirReadRequester.readAsPatientMaster(patientId);
    }

    @GetMapping("/{patientId}/fhir")
    public IBaseResource getPatientFhir(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IGenericClient fhirClient = repositoryClientFactory.newGenericClient(tenant, req);
        return fhirClient.read().resource("Patient").withId(patientId).execute();
    }

    @GetMapping("")
    public List<PatientMaster> getAllPatients(
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        List<PatientMaster> result = fhirSearchRequester.searchPatientMasterGoldenList(new SearchParameterMap());
        return result;
    }

    @GetMapping("/{patientId}/recommendation")
    public IBaseBundle getPatientRecommendation(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IGenericClient fhirClient = repositoryClientFactory.newGenericClient(tenant, req);
        return fhirClient.search()
                .forResource("ImmunizationRecommendation")
                .where(new ca.uhn.fhir.rest.gclient.ReferenceClientParam("patient").hasId(patientId))
                .execute();
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{patientId}/vaccination")
    public List<VaccinationMaster> getPatientVaccination(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        return fhirSearchRequester.searchVaccinationMasterGoldenList(
                new SearchParameterMap().add("patient", referenceParam));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{patientId}/observations")
    public List<ObservationReported> getPatientObservation(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        return fhirSearchRequester.searchObservationReportedList(
                new SearchParameterMap("subject", referenceParam));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{patientId}/related")
    public List<? extends  PatientMaster> getPatientRelatedPatients(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        if (isGolden) {
			  return fhirSearchRequester
                    .searchPatientReportedFromGoldenIdWithMdmLinks(patientId);
        } else {
            PatientMaster goldenRecord = fhirReadRequester
                    .readPatientMasterWithMdmLink(patientId);
            if (goldenRecord != null) {
					return List.of(goldenRecord);
            } else {
					return List.of();
				}
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/search")
    public List<PatientMaster> basicSearch(
            @RequestParam(required = false) String family,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String identifier,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        return fhirSearchRequester.searchPatientMasterGoldenList(
                new SearchParameterMap("family", new ca.uhn.fhir.rest.param.StringParam(family))
                        .add("name", new ca.uhn.fhir.rest.param.StringParam(name))
                        .add("identifier", new ca.uhn.fhir.rest.param.TokenParam().setValue(identifier)));
    }

    @GetMapping("/{patientId}/shLinkPayload")
    public ShLinkPayload getShLinkPayload(
            @PathVariable("patientId") String patientId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IBaseResource patientSelected = getPatientFhir(patientId, tenant, req);
        String manifestUrl = PatientShLinkRestController.getManifestUrl(req, patientSelected, tenant);
        return PatientShLinkRestController.generatePatientShLinkPayload(manifestUrl);
    }

}
