package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.MatchedTarget;
import ca.uhn.fhir.mdm.api.MdmMatchOutcome;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;

public class MdmCustomMatchFinderSvcR4 extends AbstractMdmCustomMatchFinderSvc<org.hl7.fhir.r4.model.Immunization> implements IMdmMatchFinderSvc, IMdmCustomMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	IFhirResourceDao<org.hl7.fhir.r4.model.Immunization> immunizationDao;
	@Autowired
	IFhirResourceDao<org.hl7.fhir.r4.model.Patient> patientDao;

	public MdmCustomMatchFinderSvcR4() {
		super();
	}

	public List<MatchedTarget> matchImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId) {
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
				.setValue(GOLDEN_RECORD).setModifier(TokenParamModifier.NOT));

		/*
		 * Looking for matching patient through reference and mdm operation,
		 * or with identifier
		 */
		String patientParameterValue = null;
		if (immunization.getPatient().getReference() != null) {
			patientParameterValue = immunization.getPatient().getReference();
		} else if (immunization.getPatient().getIdentifier() != null) {
			SystemRequestDetails patientRequestDetails = new SystemRequestDetails();
			patientRequestDetails.setRequestPartitionId(theRequestPartitionId);
			SearchParameterMap patientSearchParameter = new SearchParameterMap()
				.add("_tag", new TokenParam()
					.setSystem(GOLDEN_SYSTEM_TAG)
					.setValue(GOLDEN_RECORD))
				.add("identifier", new TokenParam()
					.setSystem(immunization.getPatient().getIdentifier().getSystem())
					.setValue(immunization.getPatient().getIdentifier().getValue()));
			patientParameterValue = String.join(",", patientDao.search(patientSearchParameter, patientRequestDetails).getAllResourceIds());
		} else {
			throw new InvalidRequestException("No patient specified");
		}
		searchParameterMap.add("patient", new ReferenceParam()
			.setMdmExpand(true) // Including other patients entities
			.setValue(patientParameterValue));
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
		Coding cvx = MappingHelper.filterCodeableConceptR4(immunization.getVaccineCode(), ImmunizationMapper.CVX_SYSTEM);
		if (cvx != null) {
			i1.setCVX(cvx.getCode());
		}
		if (immunization.hasManufacturer()) {
			i1.setMVX(immunization.getManufacturer().getIdentifier().getValue());
		}
		try {
			if (immunization.hasOccurrenceStringType()) {
				i1.setDate(immunization.getOccurrenceStringType().getValue()); // TODO parse correctly
			} else if (immunization.hasOccurrenceDateTimeType()) {
				i1.setDate(immunization.getOccurrenceDateTimeType().getValue());
			}
		} catch (ParseException ignored) {
//			e.printStackTrace();
		}

		i1.setLotNumber(immunization.getLotNumber());

		if (immunization.getPrimarySource()) {
			i1.setSource(ImmunizationSource.SOURCE);
//		} else if (immunization.hasInformationSource()
//			&& immunization.getInformationSource().getConcept() != null
//			&& StringUtils.isNotBlank(immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR4.INFORMATION_SOURCE))
//			&& immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR4.INFORMATION_SOURCE).equals("00")) {
//			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}

//		if (immunization.hasInformationSource()) { // TODO improve organisation naming and designation among tenancy or in resource info
//			if (immunization.getInformationSource().getReference() != null) {
//				if (immunization.getInformationSource().getReference().getIdentifier() != null) {
//					i1.setOrganisationID(immunization.getInformationSource().getReference().getIdentifier().getValue());
//				} else if (immunization.getInformationSource().getReference().getReference() != null
//					&& immunization.getInformationSource().getReference().getReference().startsWith("Organisation/")) {
//					i1.setOrganisationID(immunization.getInformationSource().getReference().getReference()); // TODO get organisation name from db
//				}
//			}
//		}
		if ((i1.getOrganisationID() == null || i1.getOrganisationID().isBlank()) && theRequestPartitionId.hasPartitionNames()) {
			i1.setOrganisationID(theRequestPartitionId.getFirstPartitionNameOrNull());
		}
		return i1;
	}

}
