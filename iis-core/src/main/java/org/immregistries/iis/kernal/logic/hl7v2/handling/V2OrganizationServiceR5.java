package org.immregistries.iis.kernal.logic.hl7v2.handling;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.Organization;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.mapping.mappers.fields.r5.BusinessIdentifierMapperR5;
import org.immregistries.iis.kernal.mapping.requesters.FhirSaveRequesterR5;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Conditional(OnR5Condition.class)
public class V2OrganizationServiceR5 extends V2OrganizationService<Organization, HL7Reader> {

	@Autowired
	private FhirSaveRequesterR5 fhirSaveRequester;
	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private BusinessIdentifierMapperR5 businessIdentifierMapper;

	public @Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, HL7Reader reader, String sendingFacilityName) throws ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		String facilityId = reader.getValue(4);

		if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
			if (!facilityId.equals(tenant.getOrganizationName())) {
				throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4", "MSH", 1, 4);
			}
		}
		Organization responsibleOrganization = null;
		if (StringUtils.isNotBlank(sendingFacilityName) && !sendingFacilityName.equals("null")) {
			responsibleOrganization = fhirSearchRequester.searchOrganizationR5(new SearchParameterMap(ORGANIZATION_SP_NAME, new StringParam(sendingFacilityName)));
			// Organization.NAME.matches().value(sendingFacilityName));
			if (responsibleOrganization == null) {
				responsibleOrganization = fhirSaveRequester.saveOrganization(new Organization().setName(sendingFacilityName));
			}
		}

		if (responsibleOrganization == null) {
			responsibleOrganization = processSendingOrganization(reader);
		}
		if (responsibleOrganization == null) {
			responsibleOrganization = processManagingOrganization(reader);
		}
		IIdType organizationIdType = null;
		if (responsibleOrganization != null) {
			organizationIdType = responsibleOrganization.getIdElement();
		}
		return organizationIdType;
	}

	public Organization processSendingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(4, 1);
		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setValue(reader.getValue(4, 2));
		// businessIdentifier.setType(reader.getValue(4, 3)); TODO support TYPE in TOKEN
		// PARAM
		TokenParam tokenParam = businessIdentifierMapper.asTokenParam(businessIdentifier);
		Organization sendingOrganization = null;
		if (tokenParam != null) {
			sendingOrganization = fhirSearchRequester.searchOrganizationR5(new SearchParameterMap(Organization.SP_IDENTIFIER, tokenParam));
		} else if (organizationName != null) {
			sendingOrganization = fhirSearchRequester.searchOrganizationR5(new SearchParameterMap(Organization.SP_NAME, new StringParam(organizationName)));
		}
		if (sendingOrganization == null && (StringUtils.isNotBlank(organizationName) || tokenParam != null)) {
			sendingOrganization = new Organization().setName(organizationName);
			if (tokenParam != null) {
				sendingOrganization.addIdentifier(businessIdentifierMapper.fhirObject(businessIdentifier));
			}
			sendingOrganization = fhirSaveRequester.saveOrganization(sendingOrganization);
		}
		return sendingOrganization;
	}

	public Organization processManagingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(22, 1);
		Organization managingOrganization = null;
		String managingIdentifier;
		managingIdentifier = reader.getValue(22, 11);
		if (StringUtils.isBlank(managingIdentifier)) {
			managingIdentifier = reader.getValue(22, 3);
		}
		if (managingIdentifier != null) {
			managingOrganization = fhirSearchRequester.searchOrganizationR5(new SearchParameterMap(Organization.SP_IDENTIFIER, new TokenParam().setSystem(reader.getValue(22, 7)).setValue(managingIdentifier)));
			// Organization.IDENTIFIER.exactly()
			// .systemAndIdentifier(reader.getValue(22, 7), managingIdentifier));
			if (managingOrganization == null) {
				managingOrganization = new Organization();
				managingOrganization.setName(organizationName);
				managingOrganization.addIdentifier().setValue(managingIdentifier).setSystem(reader.getValue(22, 7));
			}
		}
		if (managingOrganization != null) {
			managingOrganization = fhirSaveRequester.saveOrganization(managingOrganization);
		}
		return managingOrganization;
	}
}
