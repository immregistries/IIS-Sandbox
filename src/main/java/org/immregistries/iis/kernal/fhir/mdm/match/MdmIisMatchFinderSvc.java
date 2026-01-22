package org.immregistries.iis.kernal.fhir.mdm.match;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.svc.MdmMatchFinderSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.candidate.MdmCandidateSearchSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.MatchedTarget;
import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.svc.MdmResourceMatcherSvc;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.logic.match.PatientMismoConversionService;
import org.immregistries.iis.kernal.logic.match.VaccinationDedupConversionService;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.IisReference;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.mismo.match.PatientMatchResult;
import org.immregistries.mismo.match.PatientMatcher;
import org.immregistries.mismo.match.model.Patient;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.uhn.fhir.jpa.mdm.svc.candidate.CandidateSearcher.idOrType;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

/**
 * Custom, based on MdmMatchFinderSvcImpl from Hapi-fhir v6.2.4, to allow for Immunization matching with external library
 * Generics FhirImmunization Immunization
 * Generics FhirPatient Patient
 */
public class MdmIisMatchFinderSvc<FhirImmunization extends IAnyResource, FhirPatient extends IAnyResource> extends MdmMatchFinderSvcImpl implements IMdmMatchFinderSvc, IMdmIisMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	private MdmCandidateSearchSvc myMdmCandidateSearchSvc;
	@Autowired
	private MdmResourceMatcherSvc myMdmResourceMatcherSvc;
	@Autowired
	private PatientMismoConversionService<FhirPatient> patientMismoConversionService;
	@Autowired
	private PatientMapper<FhirPatient> patientMapper;
	@Autowired
	private ImmunizationMapper<FhirImmunization> immunizationMapper;
	@Autowired
	private VaccinationDedupConversionService<FhirImmunization> vaccinationDedupConversionService;

	private final PatientMatcher patientMismoMatcher;

	private IFhirResourceDao<FhirImmunization> immunizationDao;
	private IFhirResourceDao<FhirPatient> patientDao;

	@Autowired
	public void setDao(DaoRegistry daoRegistry) {
		immunizationDao = daoRegistry.getResourceDao(ResourceType.Immunization.name());
		patientDao = daoRegistry.getResourceDao(ResourceType.Patient.name());
	}


	public MdmIisMatchFinderSvc() {
		super();
		InputStream is = this.getClass().getResourceAsStream("/Mismo-Configuration.yml");
		if (is == null) {
			ourLog.error("Unable to find Mismo-Configuration file");
		}
		patientMismoMatcher = new PatientMatcher(is);
	}

	@Override
	@Nonnull
	@Transactional
	public List<MatchedTarget> getMatchedTargets(String theResourceType, IAnyResource theResource, RequestPartitionId theRequestPartitionId) {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(theRequestPartitionId.getFirstPartitionNameOrNull());
		List<MatchedTarget> matches;


		if (theResourceType.equals(ResourceType.Immunization.name())) {
			matches = matchImmunization((FhirImmunization) theResource, theRequestPartitionId);
		} else if (theResourceType.equals(ResourceType.Patient.name()) && processingFlavorSet.contains(ProcessingFlavor.MISMO)) {
			/*
			 * Flavor check activating patient Matching with Mismo match
			 */
			matches = matchMismoPatient((FhirPatient) theResource, theRequestPartitionId);
			ourLog.info("Found {} matched targets for {} using mismo", matches.size(), idOrType(theResource, theResourceType));
		} else {
			/*
			 * Original code for all cases
			 */
			Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(theResourceType, theResource, theRequestPartitionId);
			matches = targetCandidates.stream()
				.map(candidate -> new MatchedTarget(candidate, myMdmResourceMatcherSvc.getMatchResult(theResource, candidate)))
				.collect(Collectors.toList());
//			if (theResourceType.equals(ResourceType.Patient.name())) {
//				for (MatchedTarget matchedTarget : matches) {
//					DiffResult diff = patientMapper.localObject(theResource).diff(patientMapper.localObject(matchedTarget.getTarget()));
//					IisLogicInterceptor.printDiff(ourLog, diff);
//				}
//			}
		}
		ourLog.info("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
		ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
		return matches;

	}

	private @NotNull List<MatchedTarget> matchMismoPatient(FhirPatient theResource, RequestPartitionId theRequestPartitionId) {
		Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(ResourceType.Patient.name(), theResource, theRequestPartitionId);
		Patient mismoPatient = patientMismoConversionService.convert(theResource);

		List<MatchedTarget> matches = targetCandidates.stream()
			.map((candidate) -> {
				Patient mismoPatientCandidate = patientMismoConversionService.convert((FhirPatient) candidate);
				PatientMatchResult mismoMatchResult = patientMismoMatcher.match(mismoPatient, mismoPatientCandidate);
				return new MatchedTarget(candidate, IMdmIisMatchFinderSvc.mismoResultToMdmMatchOutcome(mismoMatchResult));
			}).collect(Collectors.toList());
		return matches;
	}

	public List<MatchedTarget> matchImmunization(FhirImmunization immunization, RequestPartitionId theRequestPartitionId) {
		IisReference patient = immunizationMapper.extractPatientReference(immunization);
		if (patient == null) {
			throw new InvalidRequestException("No patient specified");
		}
		Deterministic comparer = new Deterministic();
		org.immregistries.vaccination_deduplication.Immunization i1 = vaccinationDedupConversionService.convert(immunization, theRequestPartitionId);

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestPartitionId(theRequestPartitionId);

		IBundleProvider targetCandidates;
		SearchParameterMap searchParameterMap = new SearchParameterMap()
			.setLoadSynchronous(true)
			.setLoadSynchronousUpTo(1000)
			.add("_tag", new TokenParam()
				.setSystem(GOLDEN_SYSTEM_TAG)
				.setValue(GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));

		/*
		 * Looking for matching patient through reference and mdm operation,
		 * or with identifier
		 */
		String patientParameterValue = null;
		if (patient.getReference() != null) {
			patientParameterValue = patient.getReference();
		} else if (patient.getIdentifier() != null) {
			SystemRequestDetails patientRequestDetails = new SystemRequestDetails();
			patientRequestDetails.setRequestPartitionId(theRequestPartitionId);
			SearchParameterMap patientSearchParameter = new SearchParameterMap()
				.add("_tag", new TokenParam()
					.setSystem(GOLDEN_SYSTEM_TAG)
					.setValue(GOLDEN_RECORD))
				.add("identifier", new TokenParam()
					.setSystem(patient.getIdentifier().getSystem())
					.setValue(patient.getIdentifier().getValue()));
			patientParameterValue = String.join(",", patientDao.search(patientSearchParameter, patientRequestDetails).getAllResourceIds());
		} else {
			throw new InvalidRequestException("No patient specified");
		}
		searchParameterMap.add("patient", new ReferenceParam()
			.setMdmExpand(true) // Including other patients entities
			.setValue(patientParameterValue));
		targetCandidates = immunizationDao.search(searchParameterMap, requestDetails);
		return targetCandidates.getAllResources().stream()
			.map((resource) -> (FhirImmunization) resource)
			.map((immunization2) -> {
				org.immregistries.vaccination_deduplication.Immunization i2 = vaccinationDedupConversionService.convert((FhirImmunization) immunization2, theRequestPartitionId);
				ComparisonResult comparison = comparer.compare(i1, i2);
				if (comparison.equals(ComparisonResult.EQUAL)) {
					return new MatchedTarget(immunization2, MdmMatchOutcome.EID_MATCH); // TODO verify if accurate to use this match outcome
				} else {
					return null;
				}
			}).filter((Objects::nonNull)).collect(Collectors.toList());

	}


}
