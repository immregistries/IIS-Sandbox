package org.immregistries.iis.kernal.logic.hl7v2.handling;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.logic.validation.ImmunizationValidator;
import org.immregistries.iis.kernal.logic.validation.ObservationValidator;
import org.immregistries.iis.kernal.logic.validation.PatientValidator;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.r4.*;
import org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Conditional(OnR4Condition.class)
public class FhirMessagingHandler extends IncomingMessageHandler<Bundle, Object> {

	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private FhirSaveRequester fhirSaveRequester;

	@Autowired
	private PatientMapperR4 patientMapper;
	@Autowired
	private ImmunizationMapperR4 immunizationMapper;
	@Autowired
	private PractitionerMapperR4 practitionerMapper;
	@Autowired
	private ObservationMapperR4 observationMapper;
	@Autowired
	private LocationMapperR4 locationMapper;

	@Autowired
	private PatientValidator patientValidator;
	@Autowired
	private ObservationValidator observationValidator;
	@Autowired
	private ImmunizationValidator immunizationValidator;

	@Override
	public String extractMessageType(Bundle bundle) {
		return bundle.getEntry().stream()
				.filter(bundleEntryComponent -> ResourceType.MessageHeader
						.equals(bundleEntryComponent.getResource().getResourceType()))
				.findFirst()
				.map(Bundle.BundleEntryComponent::getResource)
				.map(resource -> resource.getMeta().getTagFirstRep().getCode())
				.orElse(null);
	}

	@Override
	public Bundle parseSource(String message) {
		return fhirContext.newJsonParser().parseResource(Bundle.class, message);
	}

	@Override
	public IIdType readResponsibleOrganizationIIdType(Tenant tenant, Bundle bundle, String sendingFacilityName) throws ProcessingException {
		return null;
		//TODO ? or ignore
	}

	@Override
	public PatientReported processPatient(Tenant tenant, Bundle bundle, List<IisReportable> iisReportableList,
			Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate,
			IIdType managingOrganizationId) throws ProcessingException {
		Patient patient = ((Patient) bundle.getEntry().stream()
				.filter((entry) -> entry.getResource().getResourceType().equals(ResourceType.Patient)).findFirst()
				.map(Bundle.BundleEntryComponent::getResource).orElse(null));
		PatientReported patientReported = patientMapper.localObjectReported(patient);
		patientReported.setPatientId(null);
		patientReported.setTenant(tenant);
		patientReported.setReportedDate(new Date());

		if (managingOrganizationId != null && managingOrganizationId.hasIdPart()) {
			patientReported.setManagingOrganizationId("Organization/" + managingOrganizationId.getIdPart());
		}

		patientReported = patientValidator.processAndValidatePatient(patientReported, iisReportableList,
				processingFlavorSet);
		IIncomingMessageHandler.verifyNoErrors(iisReportableList);
		patientReported.setUpdatedDate(new Date());
		patientReported = fhirSaveRequester.savePatientReported(patientReported);
		return patientReported;
	}

	@Override
	public List<VaccinationReported> processVaccinations(Bundle bundle, Tenant tenant,
			List<IisReportable> iisReportableList, PatientReported patientReported,
			Set<ProcessingFlavor> processingFlavorSet, boolean strictDate) throws ProcessingException {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>(bundle.getEntry().size());
		for (Bundle.BundleEntryComponent entryComponent : bundle.getEntry()) {
			if (entryComponent.hasResource()
					&& ResourceType.Immunization.equals(entryComponent.getResource().getResourceType())) {

				Immunization immunization = (Immunization) entryComponent.getResource();

				OrgLocation orgLocation = processLocation(bundle, tenant, immunization.getLocation());

				// immunization.setId(null);
				immunization.setPatient(null);
				immunization.setLocation(null);
				VaccinationReported vaccinationReported = immunizationMapper.localObjectReported(immunization);
				vaccinationReported.setVaccinationId(null);
				vaccinationReported.setPatientReported(patientReported);
				vaccinationReported.setOrgLocation(orgLocation);
				vaccinationReported.setReportedDate(new Date());
				vaccinationReported.setUpdatedDate(new Date());
				vaccinationReported.setPatientReported(patientReported);

				for (Immunization.ImmunizationPerformerComponent performer : immunization.getPerformer()) {
					ModelPerson modelPerson = processPersonPractitioner(bundle, tenant, performer.getActor());
					if (modelPerson == null || !performer.hasFunction()) {
						break;
					}
					for (Coding function : performer.getFunction().getCoding()) {
						switch (function.getCode()) {
							case ImmunizationMapper.ENTERING_VALUE: {
								vaccinationReported.setEnteredBy(modelPerson);
								break;
							}
							case ImmunizationMapper.ORDERING_VALUE: {
								vaccinationReported.setOrderingProvider(modelPerson);
								break;
							}
							case ImmunizationMapper.ADMINISTERING_VALUE: {
								vaccinationReported.setAdministeringProvider(modelPerson);
								break;
							}
						}
					}
				}
				vaccinationReported = immunizationValidator.processAndValidateVaccinationReported(
						vaccinationReported, iisReportableList, processingFlavorSet, -1, -1, -1, null);
				vaccinationReported = fhirSaveRequester.saveVaccinationReported(vaccinationReported);
				vaccinationReportedList.add(vaccinationReported);
			}
		}
		return vaccinationReportedList;
	}

	@Override
	public String processORU(Tenant tenant, Bundle bundle, String message, IIdType managingOrganizationId) {
		throw new RuntimeException("Only VXU is supported for now");
	}

	@Override
	public String processQBP(Tenant tenant, Bundle bundle, String messageReceived, IIdType managingOrganizationId)
			throws Exception {
		throw new RuntimeException("Only VXU is supported for now");
	}

	@Override
	public String buildResultWithoutValidation(Bundle bundle, List<IisReportable> iisReportableList,
			Set<ProcessingFlavor> processingFlavorSet) {
		Bundle resultBundle = new Bundle();
		/*
		 * TODO MessageHeader
		 */
		OperationOutcome operationOutcome = new OperationOutcome();
		resultBundle.addEntry().setResource(operationOutcome);
		for (IisReportable reportable : iisReportableList) {
			OperationOutcome.OperationOutcomeIssueComponent issueComponent = getIssueComponent(reportable);
			operationOutcome.addIssue(issueComponent);
		}

		return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resultBundle);
	}

	private static OperationOutcome.@NotNull OperationOutcomeIssueComponent getIssueComponent(
			IisReportable reportable) {
		OperationOutcome.OperationOutcomeIssueComponent issueComponent = new OperationOutcome.OperationOutcomeIssueComponent();
		switch (reportable.getSeverity()) {
			case ERROR: {
				issueComponent.setSeverity(OperationOutcome.IssueSeverity.ERROR);
				break;
			}
			case WARN: {
				issueComponent.setSeverity(OperationOutcome.IssueSeverity.WARNING);
				break;
			}
			case ACCEPT:
			case NOTICE:
			case INFO: {
				issueComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
				break;
			}
		}
		issueComponent.setCode(OperationOutcome.IssueType.VALUE);
		CodeableConcept details = new CodeableConcept();
		issueComponent.setDetails(details);
		CodedWithExceptions codedWithExceptions = reportable.getApplicationErrorCode();
		details.addCoding(new Coding(codedWithExceptions.getNameOfCodingSystem(),
				codedWithExceptions.getIdentifier(),
				codedWithExceptions.getText()));
		details.addCoding(new Coding("Source", reportable.getSource().name(), reportable.getSource().name()));
		issueComponent.setLocation(
				reportable
						.getHl7LocationList().stream()
						.filter(Objects::nonNull)
						.map(Hl7Location::toString)
						.map(StringType::new)
						.collect(Collectors.toList()));
		issueComponent.setDiagnostics(reportable.getDiagnosticMessage());
		return issueComponent;
	}

	@Override
	public String buildResultWithValidation(Bundle bundle, Object o, List<IisReportable> iisReportableList,
			Set<ProcessingFlavor> processingFlavorSet) {
		return buildResultWithoutValidation(bundle, iisReportableList, processingFlavorSet);
	}

	@Override
	public Object validation(String message, List<IisReportable> iisReportableList) throws Exception {
		return null;
	}

	public OrgLocation processLocation(Bundle bundle, Tenant tenant, Reference reference) {
		return bundle.getEntry().stream()
				.filter(bundleEntryComponent -> reference.getReference().equals(bundleEntryComponent.getFullUrl())
						|| reference.getReference().equals(bundleEntryComponent.getResource().getId()))
				.findFirst()
				.map(Bundle.BundleEntryComponent::getResource)
				.map(resource -> locationMapper.localObject((Location) resource))
				.map(orgLocation -> {
					orgLocation.setTenant(tenant);
					return orgLocation;
				})
				.map(orgLocation -> fhirSaveRequester.saveOrgLocation(orgLocation))
				.orElse(null);
	}

	public ModelPerson processPersonPractitioner(Bundle bundle, Tenant tenant, Reference reference) {
		if (reference.getReferenceElement().getResourceType().equals("Practitioner")) {
			return bundle.getEntry().stream()
					.filter(bundleEntryComponent -> reference.getReference().equals(bundleEntryComponent.getFullUrl())
							|| reference.getReference().equals(bundleEntryComponent.getResource().getId()))
					.findFirst()
					.map(Bundle.BundleEntryComponent::getResource)
					.map(resource -> practitionerMapper.localObject((Practitioner) resource))
					.map(modelPerson -> {
						modelPerson.setTenant(tenant);
						return modelPerson;
					})
					.map(modelPerson -> fhirSaveRequester.savePractitioner(modelPerson))
					.orElse(null);
		} else if (reference.getReferenceElement().getResourceType().equals("PractitionerRole")) {
			Optional<Reference> practitionerReference = bundle.getEntry().stream()
					.filter(bundleEntryComponent -> reference.getReference().equals(bundleEntryComponent.getFullUrl())
							|| reference.getReference().equals(bundleEntryComponent.getResource().getId()))
					.findFirst()
					.map(Bundle.BundleEntryComponent::getResource)
					.map(resource -> (PractitionerRole) resource)
					.map(PractitionerRole::getPractitioner);
			if (practitionerReference.isPresent()) {
				return processPersonPractitioner(bundle, tenant, practitionerReference.get());
			} else {
				return null;
			}
		}
		return null;
	}
}
