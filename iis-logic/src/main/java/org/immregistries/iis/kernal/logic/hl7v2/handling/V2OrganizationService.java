package org.immregistries.iis.kernal.logic.hl7v2.handling;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequester;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class V2OrganizationService<FhirOrganization extends IAnyResource, ParsedSource> {
	public static final String ORGANIZATION_SP_NAME = org.hl7.fhir.r4.model.Organization.SP_NAME;
	public static final String ORGANIZATION_SP_IDENTIFIER = org.hl7.fhir.r4.model.Organization.SP_IDENTIFIER;

	@Autowired
	private FhirSaveRequester fhirSaveRequester;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private BusinessIdentifierMapper businessIdentifierMapper;

	abstract @Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, ParsedSource parsedSource, String sendingFacilityName) throws ProcessingException;

	abstract FhirOrganization processSendingOrganization(ParsedSource parsedSource);

	abstract FhirOrganization processManagingOrganization(ParsedSource parsedSource);
}
