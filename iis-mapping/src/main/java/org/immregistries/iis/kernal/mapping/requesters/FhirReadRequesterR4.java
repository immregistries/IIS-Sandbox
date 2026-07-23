package org.immregistries.iis.kernal.mapping.requesters;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.IisFhirClientFactory;
import org.immregistries.iis.kernal.mapping.MappingService;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.LocationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.PractitionerMapper;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Conditional(OnR4Condition.class)
public class FhirReadRequesterR4 implements IFhirReadRequester {

	@Autowired
	private IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	private DaoRegistry daoRegistry;
	@Autowired
	private MappingService mappingService;
	@Autowired
	private RequestTenantUtil requestTenantUtil;

	/**
	 *
	 * @param fhirType FHIR Resource Class
	 * @param id       resource id
	 * @return resource found
	 */
	@Override
	public IBaseResource read(String fhirType, String id) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(fhirType);
		return dao.read(new IdType(id), requestTenantUtil.requestDetailsWithPartitionName());
	}


	@Override
	public IisPatient readAsPatient(String id) {
		Patient patient = (Patient) read(PatientMapper.PATIENT_FHIR_TYPE_NAME, id);
		return (IisPatient) mappingService.localObject(patient);
	}

	@Override
	public PatientMaster readAsPatientMaster(String id) {
		Patient patient = (Patient) read(PatientMapper.PATIENT_FHIR_TYPE_NAME, id);
		if (FhirRequesterUtil.isGoldenRecord(patient)) {
			return (PatientMaster) mappingService.localObjectMaster(patient);
		}
		return null;
	}

	@Override
	public PatientReported readAsPatientReported(String id) {
		return (PatientReported) mappingService.localObjectReportedWithMaster((IAnyResource) read(PatientMapper.PATIENT_FHIR_TYPE_NAME, id));
	}

	@Override
	public ModelPerson readPractitionerAsPerson(String id) {
		return (ModelPerson) mappingService.localObject((Practitioner) read(PractitionerMapper.PRACTITIONER_FHIR_TYPE_NAME, id));
	}

	@Override
	public OrgLocation readAsOrgLocation(String id) {
		return (OrgLocation) mappingService.localObject((Location) read(LocationMapper.LOCATION_FHIR_TYPE_NAME, id));
	}

	@Override
	public VaccinationReported readAsVaccinationReported(String id) {
		return (VaccinationReported) mappingService.localObjectReportedWithMaster((Immunization) read(ImmunizationMapper.IMMUNIZATION_FHIR_TYPE_NAME, id));
	}

	@Override
	public IisVaccination readAsVaccination(String id) {
		Immunization immunization = (Immunization) read(ImmunizationMapper.IMMUNIZATION_FHIR_TYPE_NAME, id);
		return (IisVaccination) mappingService.localObject(immunization);
	}

	@Override
	public VaccinationMaster readAsVaccinationMaster(String id) {
		Immunization immunization = (Immunization) read(ImmunizationMapper.IMMUNIZATION_FHIR_TYPE_NAME, id);
		if (FhirRequesterUtil.isGoldenRecord(immunization)) {
			return (VaccinationMaster) mappingService.localObjectMaster(immunization);
		}
		return null;
	}

	@Override
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

	@Override
	public PatientMaster readPatientMasterWithMdmLink(String patientId) {
		Optional<String> goldenId = readGoldenResourceId(patientId);
		return goldenId.map(this::readAsPatientMaster).orElse(null);
	}

	@Override
	public VaccinationMaster readVaccinationMasterWithMdmLink(String vaccinationReportedId) {
		Optional<String> goldenId = readGoldenResourceId(vaccinationReportedId);
		return goldenId.map(this::readAsVaccinationMaster).orElse(null);
	}


	@Override
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
