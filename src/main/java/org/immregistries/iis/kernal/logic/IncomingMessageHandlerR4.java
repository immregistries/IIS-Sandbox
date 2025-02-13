package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester.GOLDEN_SYSTEM_TAG;

@Service
@Conditional(OnR4Condition.class)
public class IncomingMessageHandlerR4 extends IncomingMessageHandler {

	public @Nullable IIdType readResponsibleOrganizationIIdType(Tenant tenant, HL7Reader reader, String sendingFacilityName, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		String facilityId = reader.getValue(4);

		if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
			if (!facilityId.equals(tenant.getOrganizationName())) {
				throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4", "MSH", 1, 4);
			}
		}
		Organization responsibleOrganization = null;
		if (StringUtils.isNotBlank(sendingFacilityName) && !sendingFacilityName.equals("null")) {
			responsibleOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_NAME, new StringParam(sendingFacilityName)));
//					Organization.NAME.matches().value(sendingFacilityName));
			if (responsibleOrganization == null) {
				responsibleOrganization = (Organization) fhirRequester.saveOrganization(new Organization().setName(sendingFacilityName));
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

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<VaccinationMaster> vaccinationMasterList;
		{
			vaccinationMasterList = new ArrayList<>();
			Map<String, VaccinationMaster> map = new HashMap<>();
			try {
				Bundle bundle = fhirClient.search().forResource(Immunization.class).where(Immunization.PATIENT.hasId(patient.getPatientId())).withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD).sort().ascending(Immunization.IDENTIFIER).returnBundle(Bundle.class).execute();
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					Immunization immunization = (Immunization) entry.getResource();
					if (immunization.getOccurrenceDateTimeType() != null) {
						String key = sdf.format(immunization.getOccurrenceDateTimeType().getValue());
						if (immunization.getVaccineCode() != null && StringUtils.isNotBlank(immunization.getVaccineCode().getText())) {
							key += key + immunization.getVaccineCode().getText();
							VaccinationMaster vaccinationMaster = immunizationMapper.localObject(immunization);
							map.put(key, vaccinationMaster);
						}
					}
				}
			} catch (ResourceNotFoundException ignored) {
			}

			List<String> keyList = new ArrayList<>(map.keySet());
			Collections.sort(keyList);
			for (String key : keyList) {
				vaccinationMasterList.add(map.get(key));
			}

		}
		return vaccinationMasterList;
	}

	private Organization processSendingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(4, 1);
		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setValue(reader.getValue(4, 2));
//		businessIdentifier.setType(reader.getValue(4, 3)); TODO support TYPE in TOKEN PARAM
		TokenParam tokenParam = businessIdentifier.asTokenParam();
		Organization sendingOrganization = null;
		if (tokenParam != null) {
			sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_IDENTIFIER, tokenParam));
		} else if (organizationName != null) {
			sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_NAME, new StringParam(organizationName)));
		}
		if (sendingOrganization == null && (StringUtils.isNotBlank(organizationName) || tokenParam != null)) {
			sendingOrganization = new Organization()
				.setName(organizationName);
			if (tokenParam != null) {
				sendingOrganization.addIdentifier(businessIdentifier.toR4());
			}
			sendingOrganization = (Organization) fhirRequester.saveOrganization(sendingOrganization);
		}
		return sendingOrganization;
	}

	public Organization processManagingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(22, 1);
		Organization managingOrganization = null;
		String managingIdentifier = null;
		managingIdentifier = reader.getValue(22, 11);
		if (StringUtils.isBlank(managingIdentifier)) {
			managingIdentifier = reader.getValue(22, 3);
		}
		if (managingIdentifier != null) {
			managingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_IDENTIFIER, new TokenParam().setSystem(reader.getValue(22, 7)).setValue(managingIdentifier)));
//				Organization.IDENTIFIER.exactly()
//				.systemAndIdentifier(reader.getValue(22, 7), managingIdentifier));
			if (managingOrganization == null) {
				managingOrganization = new Organization();
				managingOrganization.setName(organizationName);
				managingOrganization.addIdentifier().setValue(managingIdentifier).setSystem(reader.getValue(22, 7));
			}
		}
		if (managingOrganization != null) {
			managingOrganization = (Organization) fhirRequester.saveOrganization(managingOrganization);
		}
		return managingOrganization;
	}
}
