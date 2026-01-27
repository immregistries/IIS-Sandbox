package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.controllers.rest.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.mapping.requesters.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.requesters.IFhirSaveRequester;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.immregistries.iis.kernal.controllers.RestConstants;
import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

@RestController
@RequestMapping(RestConstants.Path.REST_TENANT_PATH + RestConstants.Path.VACCINATION_PATH)
public class VaccinationRestController extends BaseTenantTiedRest {

    @Autowired
    @SuppressWarnings("rawtypes")
    private IFhirSaveRequester fhirRequester;
    @Autowired
    private FhirReadRequester fhirReadRequester;
    @Autowired
    private FhirSearchRequester fhirSearchRequester;

	@GetMapping(RestConstants.PathVariable.PlaceHolder.VACCINATION_ID_PLACEHOLDER)
    public IisVaccination getVaccination(
            @RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
            @PathVariable(RestConstants.PathVariable.Key.VACCINATION_ID) String vaccinationId,
            HttpServletRequest req) {
        return fhirReadRequester.readAsVaccination(vaccinationId);
    }

    @GetMapping()
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

    @GetMapping(RestConstants.PathVariable.PlaceHolder.VACCINATION_ID_PLACEHOLDER + "/related")
    public List<? extends IisVaccination> getRelatedVaccinations(
            @PathVariable(RestConstants.PathVariable.Key.VACCINATION_ID) String vaccinationId,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = RestConstants.Param.MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden) {
        ReferenceParam referenceParam = new ReferenceParam().setValue(vaccinationId);
        referenceParam.setMdmExpand(isGolden);
        if (isGolden) {
            return fhirSearchRequester.searchVaccinationReportedFromGoldenIdWithMdmLinks(vaccinationId);
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
