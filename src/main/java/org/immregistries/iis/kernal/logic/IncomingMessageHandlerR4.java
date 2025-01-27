package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.mqe.validator.MqeMessageServiceResponse;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

@Service
@Conditional(OnR4Condition.class)
public class IncomingMessageHandlerR4 extends IncomingMessageHandler {

	@Override
	public String process(String message, Tenant tenant, String sendingFacilityName) {
		HL7Reader reader = new HL7Reader(message);
		String messageType = reader.getValue(9);
		String responseMessage;
		partitionCreationInterceptor.getOrCreatePartitionId(tenant.getOrganizationName());
		Set<ProcessingFlavor> processingFlavorSet = null;
		try {
			processingFlavorSet = tenant.getProcessingFlavorSet();
			String facilityId = reader.getValue(4);

			if (processingFlavorSet.contains(ProcessingFlavor.SOURSOP)) {
				if (!facilityId.equals(tenant.getOrganizationName())) {
					throw new ProcessingException("Not allowed to submit for facility indicated in MSH-4", "MSH", 1, 4);
				}
			}
			Organization sendingOrganization = null;
			if (StringUtils.isNotBlank(sendingFacilityName) && !sendingFacilityName.equals("null")) {
				sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_NAME, new StringParam(sendingFacilityName)));
//					Organization.NAME.matches().value(sendingFacilityName));
				if (sendingOrganization == null) {
					sendingOrganization = (Organization) fhirRequester.saveOrganization(new Organization().setName(sendingFacilityName));
				}
			}

			if (sendingOrganization == null) {
				sendingOrganization = processSendingOrganization(reader);
			}
			if (sendingOrganization == null) {
				sendingOrganization = processManagingOrganization(reader);
			}
			switch (messageType) {
				case "VXU":
					responseMessage = processVXU(tenant, reader, message, sendingOrganization);
					break;
				case "ORU":
					responseMessage = processORU(tenant, reader, message, sendingOrganization);
					break;
				case "QBP":
					responseMessage = processQBP(tenant, reader, message);
					break;
				default:
					ProcessingException pe = new ProcessingException("Unsupported message", "", 0, 0);
					List<IisReportable> iisReportableList = List.of(IisReportable.fromProcessingException(pe));
					responseMessage = buildAck(reader, iisReportableList, processingFlavorSet);
					recordMessageReceived(message, null, responseMessage, "Unknown", "NAck", tenant);
					break;
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
			List<IisReportable> iisReportableList = new ArrayList<>();
			iisReportableList.add(IisReportable.fromProcessingException(new ProcessingException("Internal error prevented processing: " + e.getMessage(), null, 0, 0)));
			responseMessage = buildAck(reader, iisReportableList, processingFlavorSet);
		}
		return responseMessage;
	}

	@SuppressWarnings("unchecked")
	public String processVXU(Tenant tenant, HL7Reader reader, String message, Organization managingOrganization) throws Exception {
		List<IisReportable> iisReportableList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		MqeMessageServiceResponse mqeMessageServiceResponse = mqeMessageService.processMessage(message);
		List<IisReportable> nistReportables = nistValidation(message, "VXU");

		try {
			CodeMap codeMap = CodeMapManager.getCodeMap();
			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, reader, iisReportableList, processingFlavorSet, codeMap, strictDate, managingOrganization);

			List<VaccinationReported> vaccinationReportedList = processVaccinations(tenant, reader, iisReportableList, patientReported, strictDate, codeMap, processingFlavorSet);
			String ack = buildAckMqe(reader, mqeMessageServiceResponse, iisReportableList, processingFlavorSet, nistReportables);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			if (!iisReportableList.contains(e)) {
				iisReportableList.add(IisReportable.fromProcessingException(e));
			}
			String ack = buildAckMqe(reader, mqeMessageServiceResponse, iisReportableList, processingFlavorSet, nistReportables);
			recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}

	}

	@SuppressWarnings("unchecked")
	public PatientReported processPatient(Tenant tenant, HL7Reader reader, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, CodeMap codeMap, boolean strictDate, Organization managingOrganization) throws ProcessingException {
		String patientReportedExternalLink = "";
		String patientReportedAuthority = "";
		String patientReportedType = "MR";
		if (reader.advanceToSegment("PID")) {
			patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
			patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
			if (StringUtils.isBlank(patientReportedExternalLink)) {
				patientReportedAuthority = "";
				patientReportedType = "PT";
				patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
				patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
				if (StringUtils.isBlank(patientReportedExternalLink)) {
					patientReportedAuthority = "";
					patientReportedType = "PI";
					patientReportedExternalLink = reader.getValueBySearchingRepeats(3, 1, patientReportedType, 5);
					patientReportedAuthority = reader.getValueBySearchingRepeats(3, 4, patientReportedType, 5);
					if (StringUtils.isBlank(patientReportedExternalLink)) {
						throw new ProcessingException("MRN was not found, required for accepting vaccination report", "PID", 1, 3);
					}
				}
			}
		} else {
			throw new ProcessingException("No PID segment found, required for accepting vaccination report", "", 0, 0);
		}
//		PatientIdentifier patientIdentifier = new PatientIdentifier();
//		patientIdentifier.setSystem(patientReportedAuthority);
//		patientIdentifier.setValue(patientReportedExternalLink);
//		patientIdentifier.setType("MR");
		PatientReported patientReported = fhirRequester.searchPatientReported(new SearchParameterMap("identifier", new TokenParam().setValue(patientReportedExternalLink)));

		if (patientReported == null) {
			patientReported = new PatientReported();
			patientReported.setTenant(tenant);
//			patientReported.setExternalLink(patientReportedExternalLink); now dealt with in agnostic method
			patientReported.setReportedDate(new Date());
			if (managingOrganization != null) {
				patientReported.setManagingOrganizationId("Organization/" + managingOrganization.getIdElement().getIdPart());
			}
		}

		return processPatientFhirAgnostic(reader, iisReportableList, processingFlavorSet, codeMap, strictDate, patientReported);
	}

	public String processORU(Tenant tenant, HL7Reader reader, String message, Organization managingOrganization) {
		List<IisReportable> iisReportableList = new ArrayList<>();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		try {
			CodeMap codeMap = CodeMapManager.getCodeMap();

			boolean strictDate = !processingFlavorSet.contains(ProcessingFlavor.CANTALOUPE);
			PatientReported patientReported = processPatient(tenant, reader, iisReportableList, processingFlavorSet, codeMap, strictDate, managingOrganization);

			int orcCount = 0;
			int obxCount = 0;
			while (reader.advanceToSegment("ORC")) {
				orcCount++;
				if (reader.advanceToSegment("OBR", "ORC")) {
					obxCount = readAndCreateObservations(reader, iisReportableList, patientReported, strictDate, obxCount, null, null);
				} else {
					throw new ProcessingException("OBR segment was not found after ORC segment", "ORC", orcCount, 0);
				}
			}
			String ack = buildAck(reader, iisReportableList, processingFlavorSet);
			recordMessageReceived(message, patientReported, ack, "Update", "Ack", tenant);
			return ack;
		} catch (ProcessingException e) {
			if (!iisReportableList.contains(e)) {
				iisReportableList.add(IisReportable.fromProcessingException(e));
			}
			String ack = buildAck(reader, iisReportableList, processingFlavorSet);
			recordMessageReceived(message, null, ack, "Update", "Exception", tenant);
			return ack;
		}
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
			} catch (ResourceNotFoundException e) {
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
		Organization sendingOrganization = (Organization) fhirRequester.searchOrganization(new SearchParameterMap(Organization.SP_IDENTIFIER, new TokenParam().setSystem(reader.getValue(4, 10)).setValue(reader.getValue(4, 2))));
//			Organization.IDENTIFIER.exactly()
//			.systemAndIdentifier(reader.getValue(4, 10), reader.getValue(4, 2)));
		if (sendingOrganization == null && StringUtils.isNotBlank(organizationName)) {
			sendingOrganization = new Organization().setName(organizationName).addIdentifier(new Identifier().setSystem(reader.getValue(4, 2)).setValue(reader.getValue(4, 10)));
			sendingOrganization = (Organization) fhirRequester.saveOrganization(sendingOrganization);
		}
		return sendingOrganization;
	}

	public Organization processManagingOrganization(HL7Reader reader) {
		String organizationName = reader.getValue(22, 1);
		Organization managingOrganization = null;
		String managingIdentifier = null;
		if (StringUtils.isNotBlank(reader.getValue(22, 11))) {
			managingIdentifier = reader.getValue(22, 11);
		} else if (StringUtils.isNotBlank(reader.getValue(22, 3))) {
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
