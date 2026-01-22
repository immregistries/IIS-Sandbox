package org.immregistries.iis.kernal.logic.hl7v2.handling;

import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.r4.BusinessIdentifierMapperR4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
@SuppressWarnings({ "unchecked" })
public class V2IncomingMessageHandlerR4 extends V2IncomingMessageHandler {
	@Autowired
	private BusinessIdentifierMapperR4 businessIdentifierMapper;
//
//	public @Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, HL7Reader reader,
//			String sendingFacilityName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
//		String facilityId = reader.getValue(4);
//
//		if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
//			if (!facilityId.equals(tenant.getOrganizationName())) {
//				throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4", "MSH", 1, 4);
//			}
//		}
//		Organization responsibleOrganization = null;
//		if (StringUtils.isNotBlank(sendingFacilityName) && !sendingFacilityName.equals("null")) {
//			responsibleOrganization = (Organization) fhirSearchRequester.searchOrganizationR4(
//					new SearchParameterMap(Organization.SP_NAME, new StringParam(sendingFacilityName)));
//			// Organization.NAME.matches().value(sendingFacilityName));
//			if (responsibleOrganization == null) {
//				responsibleOrganization = (Organization) fhirSaveRequester
//						.saveOrganization(new Organization().setName(sendingFacilityName));
//			}
//		}
//
//		if (responsibleOrganization == null) {
//			responsibleOrganization = processSendingOrganization(reader);
//		}
//		if (responsibleOrganization == null) {
//			responsibleOrganization = processManagingOrganization(reader);
//		}
//		IIdType organizationIdType = null;
//		if (responsibleOrganization != null) {
//			organizationIdType = responsibleOrganization.getIdElement();
//		}
//		return organizationIdType;
//	}
//
//	private Organization processSendingOrganization(HL7Reader reader) {
//		String organizationName = reader.getValue(4, 1);
//		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
//		businessIdentifier.setValue(reader.getValue(4, 2));
//		// businessIdentifier.setType(reader.getValue(4, 3)); TODO support TYPE in TOKEN
//		// PARAM
//		TokenParam tokenParam = businessIdentifierMapper.asTokenParam(businessIdentifier);
//		Organization sendingOrganization = null;
//		if (tokenParam != null) {
//			sendingOrganization = (Organization) fhirSearchRequester
//					.searchOrganizationR4(new SearchParameterMap(Organization.SP_IDENTIFIER, tokenParam));
//		} else if (organizationName != null) {
//			sendingOrganization = (Organization) fhirSearchRequester.searchOrganizationR4(
//					new SearchParameterMap(Organization.SP_NAME, new StringParam(organizationName)));
//		}
//		if (sendingOrganization == null && (StringUtils.isNotBlank(organizationName) || tokenParam != null)) {
//			sendingOrganization = new Organization()
//					.setName(organizationName);
//			if (tokenParam != null) {
//				sendingOrganization.addIdentifier(businessIdentifierMapper.fhirObject(businessIdentifier));
//			}
//			sendingOrganization = (Organization) fhirSaveRequester.saveOrganization(sendingOrganization);
//		}
//		return sendingOrganization;
//	}
//
//	public Organization processManagingOrganization(HL7Reader reader) {
//		String organizationName = reader.getValue(22, 1);
//		Organization managingOrganization = null;
//		String managingIdentifier;
//		managingIdentifier = reader.getValue(22, 11);
//		if (StringUtils.isBlank(managingIdentifier)) {
//			managingIdentifier = reader.getValue(22, 3);
//		}
//		if (managingIdentifier != null) {
//			managingOrganization = (Organization) fhirSearchRequester
//					.searchOrganizationR4(new SearchParameterMap(Organization.SP_IDENTIFIER,
//							new TokenParam().setSystem(reader.getValue(22, 7)).setValue(managingIdentifier)));
//			// Organization.IDENTIFIER.exactly()
//			// .systemAndIdentifier(reader.getValue(22, 7), managingIdentifier));
//			if (managingOrganization == null) {
//				managingOrganization = new Organization();
//				managingOrganization.setName(organizationName);
//				managingOrganization.addIdentifier().setValue(managingIdentifier).setSystem(reader.getValue(22, 7));
//			}
//		}
//		if (managingOrganization != null) {
//			managingOrganization = (Organization) fhirSaveRequester.saveOrganization(managingOrganization);
//		}
//		return managingOrganization;
//	}
}
