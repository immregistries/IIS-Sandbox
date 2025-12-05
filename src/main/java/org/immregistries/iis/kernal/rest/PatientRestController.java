package org.immregistries.iis.kernal.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.HibernateConfig;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.iis.kernal.servlet.RecommendationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.immregistries.iis.kernal.servlet.PatientServletUtil.printPatientList;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/patientMaster")
public class PatientRestController extends BaseTenantTiedRest {

    private static final String MDM_EXPAND_REST_PARAM = "isGolden";
    @Autowired
    private RepositoryClientFactory repositoryClientFactory;
    @Autowired
    private AbstractFhirRequester fhirRequester;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private PatientMapper patientMapper;

    @GetMapping("/{patientId}")
    public PatientMaster getPatient(
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        return fhirRequester.readAsPatientMaster(patientId);
    }

    @GetMapping("/{patientId}/fhir")
    public IBaseResource getPatientFhir(
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        IGenericClient fhirClient = repositoryClientFactory.newGenericClient(tenant, req);
        return fhirClient.read().resource("Patient").withId(patientId).execute();
    }

    @GetMapping("")
    public List<PatientMaster> getAllPatients(
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        List<PatientMaster> result = fhirRequester.searchPatientMasterGoldenList(new SearchParameterMap());
        return result;
    }

    @GetMapping("/{patientId}/recommendation")
    public IBaseBundle getPatientRecommendation(
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
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
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        return fhirRequester.searchVaccinationMasterGoldenList(
                new SearchParameterMap().add("patient", referenceParam));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{patientId}/observations")
    public List<ObservationReported> getPatientObservation(
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        return fhirRequester.searchObservationReportedList(
                new SearchParameterMap("subject", referenceParam));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{patientId}/related")
    public List<PatientMaster> getPatientRelatedPatients(
            @PathVariable String patientId,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
            HttpServletRequest req) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
        referenceParam.setMdmExpand(isGolden);
        List<PatientMaster> relatedPatients = List.of();
        if (isGolden) {
            relatedPatients = fhirRequester
                    .searchPatientReportedFromGoldenIdWithMdmLinks(patientId);
        } else {
            PatientMaster goldenRecord = fhirRequester
                    .readPatientMasterWithMdmLink(patientId);
            if (goldenRecord != null) {
                relatedPatients = List.of(goldenRecord);
            }
        }
        return relatedPatients;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/search")
    public List<PatientMaster> basicSearch(
            @RequestParam(required = false) String family,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String identifier,
            @RequestAttribute(TenantRequestLoggingFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            HttpServletRequest req) {
        return fhirRequester.searchPatientMasterGoldenList(
                new SearchParameterMap("family", new ca.uhn.fhir.rest.param.StringParam(family))
                        .add("name", new ca.uhn.fhir.rest.param.StringParam(name))
                        .add("identifier", new ca.uhn.fhir.rest.param.TokenParam().setValue(identifier)));
    }

}
