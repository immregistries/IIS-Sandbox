package org.immregistries.iis.kernal.mapping.internalClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.mapping.resourceMappers.*;
import org.immregistries.iis.kernal.security.TenantUtil;
import org.immregistries.iis.kernal.mapping.AllMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("rawtypes")
public abstract class FhirSaveRequester<Patient extends IBaseResource, Immunization extends IBaseResource, Location extends IBaseResource, Practitioner extends IBaseResource, Observation extends IBaseResource, Person extends IBaseResource, Organization extends IBaseResource, RelatedPerson extends IBaseResource>
		implements
		IFhirRequester<Patient, Immunization, Location, Practitioner, Observation, Person, Organization, RelatedPerson> {
	// public static final String GOLDEN_SYSTEM_IDENTIFIER =
	// "\"http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id\"";
	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";
	private static final String GOLDEN_CRITERION_PART = GOLDEN_SYSTEM_TAG + "|" + GOLDEN_RECORD;
	private static final String NOT_GOLDEN_CRITERION = "_tag:not=" + GOLDEN_CRITERION_PART;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	PatientMapper<Patient> patientMapper;
	@Autowired
	ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	LocationMapper<Location> locationMapper;
	@Autowired
	PractitionerMapper<Practitioner> practitionerMapper;
	@Autowired
	ObservationMapper<Observation> observationMapper;
	@Autowired
	AllMappingService allMappingService;

	@Autowired
	IisFhirClientFactory iisFhirClientFactory;
	@Autowired
	FhirContext fhirContext;
	@Autowired
	DaoRegistry daoRegistry;


	/**
	 * Helping method for saving, executes conditional update and create on HAPI
	 * DAO, adds parameter to avoid golden records
	 *
	 * @param createOnly
	 * @param resource   Resource to save
	 * @param where      HAPIFHIR Criteria list
	 * @return methodOutcome
	 */
	protected MethodOutcome save(boolean createOnly, IBaseResource resource, ICriterion... where) {
		IFhirResourceDao dao = daoRegistry.getResourceDao(resource);
		String params = FhirRequesterUtil.stringCriterionList(fhirContext, where);
		if (StringUtils.isNotBlank(params)) {
			// If not empty add &
			params += "&";
			params += NOT_GOLDEN_CRITERION;
		}
		DaoMethodOutcome outcome;
		if (createOnly) {
			return dao.create(resource, TenantUtil.get().requestDetailsWithPartitionName());
		} else
			try {
				// IUpdateTyped updateTyped =
				// repositoryClientFactory.getFhirClient().update().resource(resource);
				// if (where.length == 0) {
				// return updateTyped.execute();
				// }
				// IUpdateWithQueryTyped updateWithQueryTyped =
				// updateTyped.conditional().where(where[0]);
				// for (int i = 1; i < where.length; i++) {
				// updateWithQueryTyped = updateWithQueryTyped.and(where[i]);
				// }
				// return updateWithQueryTyped.execute();
				return dao.update(resource, params, TenantUtil.get().requestDetailsWithPartitionName());
			} catch (InvalidRequestException invalidRequestException) {
				return dao.create(resource, TenantUtil.get().requestDetailsWithPartitionName());
			}
		// catch (JdbcBatchUpdateException jdbcBatchUpdateException) {
		// return dao.create(resource,
		// TenantUtil.get().requestDetailsWithPartitionName());
		// }
	}




}
