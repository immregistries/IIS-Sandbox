package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IDomainResource;

/**
 * Interceptor solving patient business identifier references to the references id if known
 *
 * @param <Identifier>   FHIR Identifier class
 * @param <Immunization> FHIR Immunization class
 * @param <Group>        FHIR Group class
 * @param <Observation>  FHIR Observation class
 */
public interface IIdentifierSolverInterceptor<Identifier extends IBaseElement, Immunization extends IDomainResource, Group extends IDomainResource, Observation extends IDomainResource> {
	/**
	 * Resolves patient business identifier references to the actual references id if known
	 * Supports Immunization, Group, Observation
	 * TODO support other resources
	 * TODO add flavours ?
	 */
	void handle(RequestDetails requestDetails) throws InvalidRequestException;

	void handleImmunization(RequestDetails requestDetails, Immunization immunization);

	void handleObservation(RequestDetails requestDetails, Observation Observation);

	void handleGroup(RequestDetails requestDetails, Group group);

	String solvePatientIdentifier(RequestDetails requestDetails, Identifier identifier);
}
