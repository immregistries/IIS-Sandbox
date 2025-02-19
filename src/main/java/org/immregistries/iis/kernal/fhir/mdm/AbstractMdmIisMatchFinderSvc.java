package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.mdm.svc.MdmMatchFinderSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.candidate.MdmCandidateSearchSvc;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.MatchedTarget;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.svc.MdmResourceMatcherSvc;
import org.apache.commons.lang3.builder.DiffResult;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.logic.logicInterceptors.AbstractLogicInterceptor;
import org.immregistries.iis.kernal.logic.logicInterceptors.PatientProcessingInterceptor;
import org.immregistries.iis.kernal.mapping.interfaces.PatientMapper;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.servlet.PatientMatchingDatasetConversionController;
import org.immregistries.mismo.match.PatientMatchResult;
import org.immregistries.mismo.match.PatientMatcher;
import org.immregistries.mismo.match.model.Patient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.uhn.fhir.jpa.mdm.svc.candidate.CandidateSearcher.idOrType;

/**
 * Custom, based on MdmMatchFinderSvcImpl from Hapi-fhir v6.2.4, to allow for Immunization matching with external library
 */
public abstract class AbstractMdmIisMatchFinderSvc<Immunization extends IBaseResource> extends MdmMatchFinderSvcImpl implements IMdmMatchFinderSvc, IMdmIisMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	MdmCandidateSearchSvc myMdmCandidateSearchSvc;
	@Autowired
	MdmResourceMatcherSvc myMdmResourceMatcherSvc;
	@Autowired
	private PatientMatchingDatasetConversionController patientMatchingDatasetConversionController;
	@Autowired
	PatientProcessingInterceptor patientProcessingInterceptor;
	@Autowired
	PatientMapper patientMapper;

	private final PatientMatcher patientMismoMatcher;

	public AbstractMdmIisMatchFinderSvc() {
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
		if (theResourceType.equals(ResourceType.Immunization.name())) {
			List<MatchedTarget> matches = matchImmunization((Immunization) theResource, theRequestPartitionId);
			ourLog.info("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			return matches;
		} else if (theResourceType.equals(ResourceType.Patient.name()) && processingFlavorSet.contains(ProcessingFlavor.MISMO)) {
			/*
			 * Flavor check activating patient Matching with Mismo match
			 */
			Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(theResourceType, theResource, theRequestPartitionId);
			Patient mismoPatient = patientMatchingDatasetConversionController.convertFromFhir(theResource);

			List<MatchedTarget> matches = targetCandidates.stream()
				.map((candidate) -> {
					Patient mismoPatientCandidate = patientMatchingDatasetConversionController.convertFromFhir(candidate);
					PatientMatchResult mismoMatchResult = patientMismoMatcher.match(mismoPatient, mismoPatientCandidate);
					return new MatchedTarget(candidate, IMdmIisMatchFinderSvc.mismoResultToMdmMatchOutcome(mismoMatchResult));
				}).collect(Collectors.toList());

			ourLog.info("Found {} matched targets for {} with mismo.", matches.size(), idOrType(theResource, theResourceType));
			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			return matches;
		} else {
			/*
			 * Original code for all cases
			 */
			Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(theResourceType, theResource, theRequestPartitionId);

			List<MatchedTarget> matches = targetCandidates.stream()
				.map(candidate -> new MatchedTarget(candidate, myMdmResourceMatcherSvc.getMatchResult(theResource, candidate)))
				.collect(Collectors.toList());

			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			ourLog.info("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			if (theResourceType.equals(ResourceType.Patient.name())) {
				for (MatchedTarget matchedTarget : matches) {
					DiffResult diff = patientMapper.localObject(theResource).diff(patientMapper.localObject(matchedTarget.getTarget()));
					AbstractLogicInterceptor.printDiff(ourLog, diff);
				}
			}
			return matches;
		}
	}

	abstract List<MatchedTarget> matchImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId);


}
