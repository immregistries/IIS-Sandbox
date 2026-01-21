package org.immregistries.iis.kernal.fhir.mdm.match;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.mdm.svc.MdmMatchFinderSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.candidate.MdmCandidateSearchSvc;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.MatchedTarget;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.svc.MdmResourceMatcherSvc;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.logic.match.PatientMismoConversionService;
import org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.mismo.match.PatientMatchResult;
import org.immregistries.mismo.match.PatientMatcher;
import org.immregistries.mismo.match.model.Patient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.uhn.fhir.jpa.mdm.svc.candidate.CandidateSearcher.idOrType;

/**
 * Custom, based on MdmMatchFinderSvcImpl from Hapi-fhir v6.2.4, to allow for Immunization matching with external library
 */
public abstract class MdmIisMatchFinderSvc<Immunization extends IAnyResource> extends MdmMatchFinderSvcImpl implements IMdmMatchFinderSvc, IMdmIisMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	private MdmCandidateSearchSvc myMdmCandidateSearchSvc;
	@Autowired
	private MdmResourceMatcherSvc myMdmResourceMatcherSvc;
	@Autowired
	private PatientMismoConversionService patientMismoConversionService;
	@Autowired
	private PatientMapper patientMapper;

	private final PatientMatcher patientMismoMatcher;

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
			matches = matchImmunization((Immunization) theResource, theRequestPartitionId);
		} else if (theResourceType.equals(ResourceType.Patient.name()) && processingFlavorSet.contains(ProcessingFlavor.MISMO)) {
			/*
			 * Flavor check activating patient Matching with Mismo match
			 */
			matches = matchMismoPatient(theResource, theRequestPartitionId);
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

	private @NotNull List<MatchedTarget> matchMismoPatient(IAnyResource theResource, RequestPartitionId theRequestPartitionId) {
		Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(ResourceType.Patient.name(), theResource, theRequestPartitionId);
		Patient mismoPatient = patientMismoConversionService.convert(theResource);

		List<MatchedTarget> matches = targetCandidates.stream()
			.map((candidate) -> {
				Patient mismoPatientCandidate = patientMismoConversionService.convert(candidate);
				PatientMatchResult mismoMatchResult = patientMismoMatcher.match(mismoPatient, mismoPatientCandidate);
				return new MatchedTarget(candidate, IMdmIisMatchFinderSvc.mismoResultToMdmMatchOutcome(mismoMatchResult));
			}).collect(Collectors.toList());
		return matches;
	}

	abstract List<MatchedTarget> matchImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId);


}
