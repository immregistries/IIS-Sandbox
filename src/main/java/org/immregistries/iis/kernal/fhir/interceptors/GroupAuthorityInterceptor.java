package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.util.IdentifierUtil;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r5.model.Group;
import org.hl7.fhir.r5.model.Organization;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.interceptor.Interceptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY;

/**
 * In progress
 * Aims at allowing Groups access by Facilities ruling over the managing facility
 */
@Interceptor
@Conditional(OnR5Condition.class)
@Service
public class GroupAuthorityInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final int DEFAULT_MAP_SIZE = 20;

	/**
	 * Map <tenantId,Map<parentOrgId,childrenOrgId>
	 */
	private Map<String, Map<String,String>> organizationAuthorityTree = new HashMap<>(DEFAULT_MAP_SIZE);

	@Autowired
	private FhirRequesterR5 fhirRequesterR5;
	@Autowired
	private IFhirResourceDao<Organization> organizationDao;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED,order = 1500 )
	public void handleGroup(RequestDetails requestDetails)
		throws InvalidRequestException {
		Organization sendingOrganization = new Organization(); // TODO identify sending facility
		if ((requestDetails.getOperation().equals("Create") || requestDetails.getOperation().equals("Update")) && requestDetails.getResource() instanceof Group) {
			Group group = (Group) requestDetails.getResource();
			if (group.hasManagingEntity()) {
				Organization managingOrganization = organizationFromReference(group.getManagingEntity(),requestDetails);
			}
		}
	 }

	@Hook(SERVER_PROCESSING_COMPLETED_NORMALLY)
	// TODO find right hook
	public void handleOrganization(RequestDetails requestDetails)
		throws InvalidRequestException {
		if (requestDetails.getResource() instanceof Organization) {
			Organization organization = (Organization) requestDetails.getResource();
			if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.UPDATE)) {
				if (organization.hasPartOf()) {
					Reference reference = organization.getPartOf();
					Organization parent = organizationFromReference(reference,requestDetails);
					if (Objects.nonNull(parent)) {
						addOrganizationRelationship(requestDetails.getTenantId(), parent, organization);
					}
	 			}
			}
		}
	}

	private void addOrganizationRelationship(String tenantId, Organization parent, Organization child) {
		organizationAuthorityTree.putIfAbsent(tenantId,new HashMap<>(DEFAULT_MAP_SIZE));
		organizationAuthorityTree.get(tenantId).putIfAbsent(parent.getId(), child.getId());
	}

	private Organization organizationFromReference(Reference reference, RequestDetails requestDetails) {
		Organization organization;
		if (reference.hasIdentifier()) {
			SearchParameterMap searchParameterMap = new SearchParameterMap("identifier",
				new TokenParam(IdentifierUtil.identifierDtFromIdentifier(reference.getIdentifier())));
			organization = fhirRequesterR5.searchOrganization(searchParameterMap);
		} else {
			organization = organizationDao.read(reference.getReferenceElement(),requestDetails);
		}
		return  organization;
	}
}
