package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.AllMappingService;
import org.immregistries.iis.kernal.mapping.resourceMappers.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.LocationMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper;
import org.immregistries.iis.kernal.mapping.resourceMappers.PractitionerMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class FhirReadRequester {

	@Autowired
	IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	DaoRegistry daoRegistry;
	@Autowired
	AllMappingService allMappingService;

	/**
	 *
	 * @param fhirType FHIR Resource Class
	 * @param id       resource id
	 * @return resource found
	 */
	public IBaseResource read(String fhirType, String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(fhirType);
		return dao.read(new IdType(id), TenantUtil.get().requestDetailsWithPartitionName());
	}


	public IisPatient readAsPatient(String id) {
		Patient patient = (Patient) read(PatientMapper.PATIENT, id);
		return (IisPatient) allMappingService.localObject(patient);
	}

	public PatientMaster readAsPatientMaster(String id) {
		Patient patient = (Patient) read(PatientMapper.PATIENT, id);
		if (FhirRequesterUtil.isGoldenRecord(patient)) {
			return (PatientMaster) allMappingService.localObject(patient);
		}
		return null;
	}

	public PatientReported readAsPatientReported(String id) {
		return (PatientReported) allMappingService.localObjectReportedWithMaster(read(PatientMapper.PATIENT, id));
	}

	public ModelPerson readPractitionerAsPerson(String id) {
		return (ModelPerson) allMappingService.localObject((Practitioner) read(PractitionerMapper.PRACTITIONER, id));
	}

	public OrgLocation readAsOrgLocation(String id) {
		return (OrgLocation) allMappingService.localObject((Location) read(LocationMapper.LOCATION, id));
	}

	public VaccinationReported readAsVaccinationReported(String id) {
		return (VaccinationReported) allMappingService.localObjectReportedWithMaster((Immunization) read(ImmunizationMapper.IMMUNIZATION, id));
	}

	public IisVaccination readAsVaccination(String id) {
		Immunization immunization = (Immunization) read(ImmunizationMapper.IMMUNIZATION, id);
		return (IisVaccination) allMappingService.localObject(immunization);
	}

	public VaccinationMaster readAsVaccinationMaster(String id) {
		Immunization immunization = (Immunization) read(ImmunizationMapper.IMMUNIZATION, id);
		if (FhirRequesterUtil.isGoldenRecord(immunization)) {
			return (VaccinationMaster) allMappingService.localObject(immunization);
		}
		return null;
	}

	public Optional<String> readGoldenResourceId(String reportId) {
		Parameters out = iisFhirClientFactory.getOrCreateFhirClientFromContext().operation().onServer().named("$mdm-query-links")
			.withParameters(new Parameters().addParameter("resourceId", reportId)).execute();
		List<Parameters.ParametersParameterComponent> part = out.getParameter().stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"))
			.findFirst().orElse(new Parameters.ParametersParameterComponent()).getPart();
		Optional<Parameters.ParametersParameterComponent> goldenIdComponent = part.stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName()
				.equals("goldenResourceId"))
			.findFirst();
		return goldenIdComponent
			.filter(component -> !component.getValue().isEmpty())
			.map(component -> String.valueOf(component.getValue()));
	}

	public PatientMaster readPatientMasterWithMdmLink(String patientId) {
		Optional<String> goldenId = readGoldenResourceId(patientId);
		return goldenId.map(this::readAsPatientMaster).orElse(null);
	}

	public VaccinationMaster readVaccinationMasterWithMdmLink(String vaccinationReportedId) {
		Optional<String> goldenId = readGoldenResourceId(vaccinationReportedId);
		return goldenId.map(this::readAsVaccinationMaster).orElse(null);
	}



	public Stream<String> readMdmlinksReportedIds(String masterId) {
		Parameters out = iisFhirClientFactory.getOrCreateFhirClientFromContext().operation().onServer().named("$mdm-query-links")
			.withParameters(new Parameters().addParameter("goldenResourceId", masterId)).execute();
		Stream<Parameters.ParametersParameterComponent> links = out.getParameter().stream()
			.filter(parametersParameterComponent -> parametersParameterComponent.getName().equals("link"));
		return links
			.map(link -> link.getPart()
				.stream()
				.filter(part -> part.getName().equals("sourceResourceId"))
				.findFirst()
				.map(part -> ((StringType) part.getValue()).getValue()))
			.flatMap(Optional::stream);
	}



}
