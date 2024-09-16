package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.svc.MdmMatchFinderSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.candidate.MdmCandidateSearchSvc;
import ca.uhn.fhir.mdm.api.MdmMatchResultEnum;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.MatchedTarget;
import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.rules.svc.MdmResourceMatcherSvc;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.ResourceType;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.mapping.forR5.ImmunizationMapperR5;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.servlet.PatientMatchingDatasetConversionController;
import org.immregistries.mismo.match.PatientMatchDetermination;
import org.immregistries.mismo.match.PatientMatchResult;
import org.immregistries.mismo.match.PatientMatcher;
import org.immregistries.mismo.match.model.Patient;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ca.uhn.fhir.jpa.mdm.svc.candidate.CandidateSearcher.idOrType;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

/**
 * Custom, based on MdmMatchFinderSvcImpl from Hapi-fhir v6.2.4, to allow for Immunization matching with external library
 */
public class MdmCustomMatchFinderSvcR5 extends MdmMatchFinderSvcImpl implements IMdmMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	private MdmCandidateSearchSvc myMdmCandidateSearchSvc;
	@Autowired
	private MdmResourceMatcherSvc myMdmResourceMatcherSvc;
	@Autowired
	private PatientMatchingDatasetConversionController patientMatchingDatasetConversionController;

	@Override
	@Nonnull
	@Transactional
	public List<MatchedTarget> getMatchedTargets(String theResourceType, IAnyResource theResource, RequestPartitionId theRequestPartitionId) {
		if (theResourceType.equals(ResourceType.Immunization.name())) {
			List<MatchedTarget> matches = matchImmunization((Immunization) theResource, theRequestPartitionId);
			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			return matches;
		} else if (theResourceType.equals(ResourceType.Patient.name()) && ProcessingFlavor.getProcessingStyle(theRequestPartitionId.getFirstPartitionNameOrNull()).contains(ProcessingFlavor.MISMO)) {

			/**
			 * flavour check activating patient Matching with Mismo match
			 */
			Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(theResourceType, theResource, theRequestPartitionId);
			PatientMatcher mismoMatcher = new PatientMatcher();
			Patient mismoPatient = patientMatchingDatasetConversionController.convertFromR5((org.hl7.fhir.r5.model.Patient) theResource);

			List<MatchedTarget> matches = targetCandidates.stream()
				.map((candidate) -> {
					Patient mismoPatientCandidate = patientMatchingDatasetConversionController.convertFromR5((org.hl7.fhir.r5.model.Patient) candidate);
					return new MatchedTarget(candidate, mismoResultToMdmMatchOutcome(mismoMatcher.match(mismoPatient, mismoPatientCandidate)));
				}).collect(Collectors.toList());

			ourLog.info("Found {} matched targets for {} with mismo.", matches.size(), idOrType(theResource, theResourceType));
			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			return matches;
		} else {
			Collection<IAnyResource> targetCandidates = myMdmCandidateSearchSvc.findCandidates(theResourceType, theResource, theRequestPartitionId);

			List<MatchedTarget> matches = targetCandidates.stream()
				.map(candidate -> new MatchedTarget(candidate, myMdmResourceMatcherSvc.getMatchResult(theResource, candidate)))
				.collect(Collectors.toList());

			ourLog.trace("Found {} matched targets for {}.", matches.size(), idOrType(theResource, theResourceType));
			return matches;
		}
	}

	private List<MatchedTarget> matchImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId) {
		if (immunization.getPatient() == null) {
			throw new InvalidRequestException("No patient specified");
		}
		Deterministic comparer = new Deterministic();
		org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization, theRequestPartitionId);

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestPartitionId(theRequestPartitionId);

		IBundleProvider targetCandidates;
		SearchParameterMap searchParameterMap = new SearchParameterMap()
			.add("_tag", new TokenParam()
				.setSystem(GOLDEN_SYSTEM_TAG)
				.setValue(GOLDEN_RECORD));

		/**
		 * Looking for matching patient through reference and mdm operation,
		 * or with identifier
		 */
		if (immunization.getPatient().getReference() != null) {
			searchParameterMap.add("patient", new ReferenceParam()
				.setMdmExpand(true) // Including other patients entities
				.setValue(immunization.getPatient().getReference()));
		} else if (immunization.getPatient().getIdentifier() != null) {
			searchParameterMap.add(Immunization.SP_IDENTIFIER, new TokenParam()
				.setSystem(immunization.getPatient().getIdentifier().getSystem())
				.setValue(immunization.getPatient().getIdentifier().getValue()));
		} else {
			throw new InvalidRequestException("No patient specified");
		}
		targetCandidates = immunizationDao.search(searchParameterMap, requestDetails);
		return targetCandidates.getAllResources().stream()
			.map((resource) -> (Immunization) resource)
			.map((immunization2) -> {
				org.immregistries.vaccination_deduplication.Immunization i2 = toVaccDedupImmunization(immunization2, theRequestPartitionId);
				ComparisonResult comparison = comparer.compare(i1, i2);
				if (comparison.equals(ComparisonResult.EQUAL)) {
					return new MatchedTarget(immunization2, MdmMatchOutcome.EID_MATCH); // TODO verify if accurate to use this match outcome
				} else {
					return null;
				}
			}).filter((Objects::nonNull)).collect(Collectors.toList());

	}

	private org.immregistries.vaccination_deduplication.Immunization toVaccDedupImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId) {
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		i1.setCVX(immunization.getVaccineCode().getCode(ImmunizationMapperR5.CVX));
		if (immunization.hasManufacturer()) {
			i1.setMVX(immunization.getManufacturer().getReference().getIdentifier().getValue());
		}
		try {
			if (immunization.hasOccurrenceStringType()) {
				i1.setDate(immunization.getOccurrenceStringType().getValue()); // TODO parse correctly
			} else if (immunization.hasOccurrenceDateTimeType()) {
				i1.setDate(immunization.getOccurrenceDateTimeType().getValue());
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		i1.setLotNumber(immunization.getLotNumber());

		if (immunization.getPrimarySource()) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else if (immunization.hasInformationSource()
			&& immunization.getInformationSource().getConcept() != null
			&& StringUtils.isNotBlank(immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR5.INFORMATION_SOURCE))
			&& immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR5.INFORMATION_SOURCE).equals("00")) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}

		if (immunization.hasInformationSource()) { // TODO improve organisation naming and designation among tenancy or in resource info
			if (immunization.getInformationSource().getReference() != null) {
				if (immunization.getInformationSource().getReference().getIdentifier() != null) {
					i1.setOrganisationID(immunization.getInformationSource().getReference().getIdentifier().getValue());
				} else if (immunization.getInformationSource().getReference().getReference() != null
					&& immunization.getInformationSource().getReference().getReference().startsWith("Organization/")) {
					i1.setOrganisationID(immunization.getInformationSource().getReference().getReference()); // TODO get organisation name from db
				}
			}
		}
		if ((i1.getOrganisationID() == null || i1.getOrganisationID().isBlank()) && theRequestPartitionId.hasPartitionNames()) {
			i1.setOrganisationID(theRequestPartitionId.getFirstPartitionNameOrNull());
		}
		return i1;
	}

	private MdmMatchOutcome mismoResultToMdmMatchOutcome(PatientMatchResult patientMatchResult) {
		switch (patientMatchResult.getDetermination()) {
			case MATCH: {
				return MdmMatchOutcome.EID_MATCH;
//				return MdmMatchOutcome.NEW_GOLDEN_RESOURCE_MATCH;
			}
			case POSSIBLE_MATCH: {
				return MdmMatchOutcome.POSSIBLE_MATCH;
			}
			case NO_MATCH: {
				return MdmMatchOutcome.NO_MATCH;
			}
			default: {
				return MdmMatchOutcome.NO_MATCH;
			}
		}
	}


}