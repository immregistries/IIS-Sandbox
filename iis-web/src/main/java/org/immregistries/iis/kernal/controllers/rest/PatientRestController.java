package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.service.PatientShlinkApiManifestUrlService;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestParam;
import org.immregistries.iis.kernal.controllers.IisRestPath;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.immregistries.iis.kernal.controllers.rest.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.fhir.shl.ShLinkPayload;
import org.immregistries.iis.kernal.logic.shlink.PatientShLinkGenerator;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.RecommendationMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirReadRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.IisPatient;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.*;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.PATIENT_PATH)
public class PatientRestController extends BaseTenantTiedRest {

	@Autowired
	private FhirReadRequester fhirReadRequester;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private PatientShLinkGenerator patientShlinkGenerator;
	@Autowired
	private PatientShlinkApiManifestUrlService patientShlinkApiManifestUrlService;

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER)
	public IisPatient getPatient(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
		return fhirReadRequester.readAsPatientMaster(patientId);
	}

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.FHIR_RESOURCE_PATH)
	public IAnyResource getPatientFhir(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			HttpServletRequest req) {
		IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
		return (IAnyResource) fhirClient.read().resource(PatientMapper.PATIENT_FHIR_TYPE_NAME).withId(patientId).execute();
	}

	@GetMapping("")
	public List<PatientMaster> getAllPatients(
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			HttpServletRequest req) {
		return fhirSearchRequester.searchPatientMasterGoldenList(new SearchParameterMap());
	}

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.RECOMMENDATION_PATH)
	public IBaseBundle getPatientRecommendationBundle(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			HttpServletRequest req) {
		IGenericClient fhirClient = iisFhirClientFactory.newGenericClient(tenant, req);
		return fhirClient.search()
				.forResource(RecommendationMapper.IMMUNIZATION_RECOMMENDATION_FHIR_TYPE_NAME)
				.where(new ca.uhn.fhir.rest.gclient.ReferenceClientParam("patient").hasId(patientId))
				.execute();
	}

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + VACCINATION_PATH)
	public List<VaccinationMaster> getPatientVaccination(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@RequestParam(name = IisRestParam.MDM_EXPAND, defaultValue = "false") boolean isGolden,
			HttpServletRequest req) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
		referenceParam.setMdmExpand(isGolden);
		return fhirSearchRequester.searchVaccinationMasterGoldenList(
				new SearchParameterMap().add("patient", referenceParam));
	}

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.OBSERVATIONS_PATH)
	public List<ObservationReported> getPatientObservation(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@RequestParam(name = IisRestParam.MDM_EXPAND, defaultValue = "false") boolean isGolden,
			HttpServletRequest req) {
		ReferenceParam referenceParam = new ReferenceParam().setValue(patientId);
		referenceParam.setMdmExpand(isGolden);
		return fhirSearchRequester.searchObservationReportedList(
				new SearchParameterMap("subject", referenceParam));
	}

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.RELATED_PATH)
	public List<? extends IisPatient> getPatientRelatedPatients(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@RequestParam(name = IisRestParam.MDM_EXPAND, defaultValue = "false") boolean isGolden,
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

	@GetMapping(IisPathVariable.PlaceHolder.PATIENT_ID_PLACEHOLDER + IisRestPath.BasePath.SH_LINK_PAYLOAD_PATH)
	public ShLinkPayload getShLinkPayload(
			@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId,
			@RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			HttpServletRequest req) {
		IAnyResource patientSelected = getPatientFhir(patientId, tenant, req);
		String manifestUrl = patientShlinkApiManifestUrlService.getManifestUrl(req, patientSelected, tenant);
		return patientShlinkGenerator.generatePatientShLinkPayload(manifestUrl);
	}

}
