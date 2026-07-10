package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.mapping.requesters.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH)
public class VaccinationRestController extends BaseTenantTiedRest {

    @Autowired
    private FhirReadRequester fhirReadRequester;
    @Autowired
    private FhirSearchRequester fhirSearchRequester;

	@GetMapping(IisRestPath.BasePath.VACCINATION_PATH + IisPathVariable.PlaceHolder.VACCINATION_ID_PLACEHOLDER)
    public IisVaccination getVaccination(
       @RequestAttribute(name = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @PathVariable(IisPathVariable.Key.VACCINATION_ID) String vaccinationId,
            HttpServletRequest req) {
        return fhirReadRequester.readAsVaccination(vaccinationId);
    }

	@GetMapping({IisRestPath.BasePath.VACCINATION_PATH
//		 , IisRestPath.BasePath.PATIENT_PATH + IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.VACCINATION_PATH
	})
    public List<VaccinationMaster> getVaccinations(
       @RequestAttribute(name = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
       @PathVariable(name = IisPathVariable.Key.PATIENT_ID, required = false) String patientIdPath,
       @RequestParam(name = IisRestParam.PATIENT_ID, required = false) String patientIdParam) {
        SearchParameterMap parameters = new SearchParameterMap();
		String patientId = StringUtils.defaultIfBlank(patientIdPath, patientIdParam);
        if (patientId != null && !patientId.isEmpty()) {
            parameters.add("patient", new ReferenceParam(patientId));
        }
        @SuppressWarnings("unchecked")
        List<VaccinationMaster> result = fhirSearchRequester.searchVaccinationMasterGoldenList(parameters);
        return result;
    }

	@GetMapping(IisRestPath.BasePath.VACCINATION_PATH + IisPathVariable.PlaceHolder.VACCINATION_ID_PLACEHOLDER + IisRestPath.BasePath.RELATED_PATH)
    public List<? extends IisVaccination> getRelatedVaccinations(
            @PathVariable(IisPathVariable.Key.VACCINATION_ID) String vaccinationId,
            @RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
            @RequestParam(name = IisRestParam.MDM_EXPAND, defaultValue = "false") boolean isGolden) {
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
