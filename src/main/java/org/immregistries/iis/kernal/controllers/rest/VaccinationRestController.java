package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.immregistries.iis.kernal.controllers.rest.PatientRestController.MDM_EXPAND_REST_PARAM;
import static org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/VaccinationMaster")
public class VaccinationRestController extends BaseTenantTiedRest {

    @GetMapping("/{vaccinationId}")
    public VaccinationMaster getVaccination(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @PathVariable("vaccinationId") String vaccinationId,
            HttpServletRequest req) {
        // TODO make FHIr Requester tenant aware
        return fhirReadRequester.readAsVaccinationMaster(vaccinationId);
    }

    @GetMapping("")
    public List<VaccinationMaster> getVaccinations(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @RequestParam(required = false) String patientId,
            HttpServletRequest req) {

        SearchParameterMap parameters = new SearchParameterMap();
        if (patientId != null && !patientId.isEmpty()) {
            parameters.add("patient", new ReferenceParam(patientId));
        }

        @SuppressWarnings("unchecked")
        List<VaccinationMaster> result = fhirSearchRequester.searchVaccinationMasterGoldenList(parameters);
        return result;
    }

	@GetMapping("/{vaccinationId}/related")
	public List<? extends VaccinationMaster> getPatientRelatedPatients(
		@PathVariable("vaccinationId") String vaccinationId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(vaccinationId);
		referenceParam.setMdmExpand(isGolden);
		if (isGolden) {
			return fhirSearchRequester
				.searchVaccinationReportedFromGoldenIdWithMdmLinks(vaccinationId);
		} else {
			VaccinationMaster goldenRecord = fhirReadRequester
				.readVaccinationMasterWithMdmLink(vaccinationId);
			if (goldenRecord != null) {
				return List.of(goldenRecord);
			} else {
				return List.of();
			}
		}
	}

}
