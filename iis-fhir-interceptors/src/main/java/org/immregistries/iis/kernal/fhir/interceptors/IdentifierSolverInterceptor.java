package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.immregistries.iis.kernal.fhir.IisFhirInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

/**
 * Interceptor solving patient business identifier references to the references id if found
 *
 * @param <Immunization> FHIR Immunization class
 * @param <Group>        FHIR Group class
 * @param <Observation>  FHIR Observation class
 */
public abstract class IdentifierSolverInterceptor<Patient extends IDomainResource, Immunization extends IDomainResource, Group extends IDomainResource, Observation extends IDomainResource> implements IisFhirInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String PATIENT_SP_IDENTIFIER = "identifier";
	@Autowired
	private FhirContext fhirContext;

	/**
	 * Resolves patient business identifier references to the actual references id if known
	 * Supports Immunization, Group, Observation
	 * TODO support other resources
	 * TODO add flavours ?
	 */
	@Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException {
		if (requestDetails.getResource() == null || requestDetails.getRestOperationType() == null) {
			return;
		}
		if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.UPDATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE)) {
			String resourceType = fhirContext.getResourceType(requestDetails.getResource());
			IAnyResource resource = (IAnyResource) requestDetails.getResource();
			if (Strings.CI.equals(ResourceType.Immunization.name(), resourceType)) {
				handleImmunization(requestDetails, (Immunization) resource);
			} else if (Strings.CI.equals(ResourceType.Group.name(), resourceType)) {
				handleGroup(requestDetails, (Group) resource);
			} else if (Strings.CI.equals(ResourceType.Observation.name(), resourceType)) {
				handleObservation(requestDetails, (Observation) resource);
			}
		}
	}

	/**
	 * Handle Immunization
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param immunization   Resource
	 */
	abstract void handleImmunization(RequestDetails requestDetails, Immunization immunization);

	/**
	 * Handle Observation
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param observation    Resource
	 */
	abstract void handleObservation(RequestDetails requestDetails, Observation observation);

	/**
	 * Handle Group and its members
	 *
	 * @param requestDetails requestDetails to extract RequestPartitionId From and set new resource in
	 * @param group          Resource
	 */
	abstract void handleGroup(RequestDetails requestDetails, Group group);



}
