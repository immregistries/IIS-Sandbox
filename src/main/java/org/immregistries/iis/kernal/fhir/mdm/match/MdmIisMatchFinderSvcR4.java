package org.immregistries.iis.kernal.fhir.mdm.match;

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
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.r4.model.Immunization;
import org.immregistries.iis.kernal.logic.match.VaccinationDedupConversionServiceR4;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester.GOLDEN_SYSTEM_TAG;

public class MdmIisMatchFinderSvcR4 extends MdmIisMatchFinderSvc<Immunization> implements IMdmMatchFinderSvc, IMdmIisMatchFinderSvc {
	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	@Autowired
	private IFhirResourceDao<org.hl7.fhir.r4.model.Immunization> immunizationDao;
	@Autowired
	private IFhirResourceDao<org.hl7.fhir.r4.model.Patient> patientDao;

	@Autowired
	private VaccinationDedupConversionServiceR4 vaccinationDedupConversionServiceR4;

	public MdmIisMatchFinderSvcR4() {
		super();
	}

	public List<MatchedTarget> matchImmunization(Immunization immunization, RequestPartitionId theRequestPartitionId) {
		IBaseReference patient = immunization.getPatient();
		if (patient == null) {
			throw new InvalidRequestException("No patient specified");
		}
		Deterministic comparer = new Deterministic();
		org.immregistries.vaccination_deduplication.Immunization i1 = vaccinationDedupConversionServiceR4.convert(immunization, theRequestPartitionId);

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
			.map((resource) -> (Immunization) resource)
			.map((immunization2) -> {
				org.immregistries.vaccination_deduplication.Immunization i2 = vaccinationDedupConversionServiceR4.convert(immunization2, theRequestPartitionId);
				ComparisonResult comparison = comparer.compare(i1, i2);
				if (comparison.equals(ComparisonResult.EQUAL)) {
					return new MatchedTarget(immunization2, MdmMatchOutcome.EID_MATCH); // TODO verify if accurate to use this match outcome
				} else {
					return null;
				}
			}).filter((Objects::nonNull)).collect(Collectors.toList());

	}


}
