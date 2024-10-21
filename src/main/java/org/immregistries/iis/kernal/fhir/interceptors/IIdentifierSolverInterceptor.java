package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IDomainResource;

public interface IIdentifierSolverInterceptor<Identifier, Immunization extends IDomainResource, Group extends IDomainResource> {
	/**
	 * Resolves business identifier resources to actual resources references id
	 * Currently only Immunization supported
	 * TODO support Observation and other
	 * TODO add flavours
	 */
	void handle(RequestDetails requestDetails) throws InvalidRequestException;

	void handleImmunization(RequestDetails requestDetails, Immunization immunization);

	void handleGroup(RequestDetails requestDetails, Group group);

	String solvePatientIdentifier(RequestDetails requestDetails, Identifier identifier);
}
