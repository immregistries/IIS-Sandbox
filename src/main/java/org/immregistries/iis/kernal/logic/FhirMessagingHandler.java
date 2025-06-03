package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.interceptors.PartitionCreationInterceptor;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.logicInterceptors.ImmunizationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.ObservationProcessingInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.PatientProcessingInterceptor;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.LocationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.mapping.internalClient.RepositoryClientFactory;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FhirMessagingHandler extends IncomingMessageHandler<Bundle, Object> {

	@Autowired
	AbstractFhirRequester fhirRequester;

	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	AbstractHl7MessageWriter hl7MessageWriter;
	@Autowired
	PartitionCreationInterceptor partitionCreationInterceptor;
	@Autowired
	PatientProcessingInterceptor patientProcessingInterceptor;
	@Autowired
	ObservationProcessingInterceptor observationProcessingInterceptor;
	@Autowired
	ImmunizationProcessingInterceptor immunizationProcessingInterceptor;
	@Autowired
	IncomingQueryHandler incomingQueryHandler;

	@Autowired
	MessageRecordingService messageRecordingService;
	@Autowired
	FhirContext fhirContext;

	@Autowired
	PatientMapper patientMapper;
	@Autowired
	ImmunizationMapper immunizationMapper;
	@Autowired
	ObservationMapper observationMapper;
	@Autowired
	LocationMapper locationMapper;

	@Override
	public String extractMessageType(Bundle bundle) {
		return bundle.getEntryFirstRep().getResource().getMeta().getTagFirstRep().getCode();
	}

	@Override
	public Bundle parseSource(String message) {
		return fhirContext.newJsonParser().parseResource(Bundle.class, message);
	}

	@Override
	@Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, Bundle bundle, String sendingFacilityName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		return null;
	}

	@Override
	public PatientReported processPatient(Tenant tenant, Bundle bundle, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, IIdType managingOrganizationId) throws ProcessingException {
		Patient patient = ((Patient) bundle.getEntry().stream().filter((entry) -> entry.getResource().getResourceType().equals(ResourceType.Patient)).findFirst().map(Bundle.BundleEntryComponent::getResource).orElse(null));
		PatientReported patientReported = patientMapper.localObjectReported(patient);
		patientReported.setTenant(tenant);
		patientReported = fhirRequester.savePatientReported(patientReported);
		return patientReported;
	}

	@Override
	public List<VaccinationReported> processVaccinations(Bundle bundle, Tenant tenant, List<IisReportable> iisReportableList, PatientReported patientReported, Set<ProcessingFlavor> processingFlavorSet, boolean strictDate) throws ProcessingException {
		Stream<VaccinationReported> vaccinationReportedList = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(resource -> resource.getResourceType().equals(ResourceType.Immunization))
			.map(resource -> immunizationMapper.localObjectReported(resource))
			.peek(vaccinationReported -> {
				vaccinationReported.setPatientReported(patientReported);
				vaccinationReported.setUpdatedDate(new Date());
			})
			.map(vaccinationReported -> fhirRequester.saveVaccinationReported(vaccinationReported));
		return List.of();
	}

	@Override
	public String processORU(Tenant tenant, Bundle bundle, String message, IIdType managingOrganizationId) {
		throw new RuntimeException("Only VXU is supported for now");
	}

	@Override
	public String processQBP(Tenant tenant, Bundle bundle, String messageReceived, IIdType managingOrganizationId) throws Exception {
		throw new RuntimeException("Only VXU is supported for now");
	}

	@Override
	public String buildResultWithoutValidation(Bundle bundle, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) {
		return "";
	}

	@Override
	public String buildResultWithValidation(Bundle bundle, Object o, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) {
		return "";
	}

	@Override
	public Object validation(String message, List<IisReportable> iisReportableList) throws Exception {
		return null;
	}
}
