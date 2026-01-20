package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.PatientShLinkService;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.rest.RestUrlUtil.*;

@RestController
@RequestMapping(RestUrlUtil.REST_TENANT_PATH + PATIENT_PATH)
public class PatientRestController extends BaseTenantTiedRest {

	public static final String MDM_EXPAND_REST_PARAM = "isGolden";

	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private PatientShLinkService patientShlinkService;

	@GetMapping(PATIENT_ID_PLACEHOLDER)
	public IisPatient getPatient(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
		return fhirReadRequester.readAsPatientMaster(patientId);
	}

	@GetMapping(PATIENT_ID_PLACEHOLDER + "/fhir")
	public IAnyResource getPatientFhir(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		HttpServletRequest req) {
		IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
		return (IAnyResource) fhirClient.read().resource("Patient").withId(patientId).execute();
	}

	@GetMapping("")
	public List<PatientMaster> getAllPatients(
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		HttpServletRequest req) {
		@SuppressWarnings("unchecked")
		List<PatientMaster> result = fhirSearchRequester.searchPatientMasterGoldenList(new SearchParameterMap());
		return result;
	}

	@GetMapping(PATIENT_ID_PLACEHOLDER + "/recommendation")
	public IBaseBundle getPatientRecommendationBundle(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		HttpServletRequest req) {
		IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
		return fhirClient.search()
			.forResource("ImmunizationRecommendation")
			.where(new ca.uhn.fhir.rest.gclient.ReferenceClientParam("patient").hasId(patientId))
			.execute();
	}

	@SuppressWarnings("unchecked")
	@GetMapping(PATIENT_ID_PLACEHOLDER + "/vaccination")
	public List<VaccinationMaster> getPatientVaccination(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
		HttpServletRequest req) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
		referenceParam.setMdmExpand(isGolden);
		return fhirSearchRequester.searchVaccinationMasterGoldenList(
			new SearchParameterMap().add("patient", referenceParam));
	}

	@SuppressWarnings("unchecked")
	@GetMapping(PATIENT_ID_PLACEHOLDER + "/observations")
	public List<ObservationReported> getPatientObservation(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@RequestParam(name = MDM_EXPAND_REST_PARAM, defaultValue = "false") boolean isGolden,
		HttpServletRequest req) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
		referenceParam.setMdmExpand(isGolden);
		return fhirSearchRequester.searchObservationReportedList(
			new SearchParameterMap("subject", referenceParam));
	}

	@SuppressWarnings("unchecked")
	@GetMapping(PATIENT_ID_PLACEHOLDER + "/related")
	public List<? extends IisPatient> getPatientRelatedPatients(
		@PathVariable(PATIENT_ID) String patientId,
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

	@GetMapping(PATIENT_ID_PLACEHOLDER + "/shLinkPayload")
	public ShLinkPayload getShLinkPayload(
		@PathVariable(PATIENT_ID) String patientId,
		@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		HttpServletRequest req) {
		IAnyResource patientSelected = getPatientFhir(patientId, tenant, req);
		String manifestUrl = patientShlinkService.getManifestUrl(req, patientSelected, tenant);
		return patientShlinkService.generatePatientShLinkPayload(manifestUrl);
	}

}
