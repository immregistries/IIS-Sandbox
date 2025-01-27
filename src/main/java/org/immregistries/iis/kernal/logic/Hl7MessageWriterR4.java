package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.model.EvaluationActual;
import org.immregistries.vfa.connect.model.TestEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Set;

@Service
@Conditional(OnR4Condition.class)
public class Hl7MessageWriterR4 extends Hl7MessageWriter {

	public String buildVxu(VaccinationReported vaccinationReported, Tenant tenant) {
		IGenericClient fhirClient = repositoryClientFactory.getFhirClient();
		StringBuilder sb = new StringBuilder();
		CodeMap codeMap = CodeMapManager.getCodeMap();
		Set<ProcessingFlavor> processingFlavorSet = tenant.getProcessingFlavorSet();
		PatientReported patientReported = vaccinationReported.getPatientReported();
		PatientMaster patientMaster = patientReported.getPatient();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		HL7Reader reader = new HL7Reader(
			"MSH|^~\\&|||AIRA|IIS Sandbox|20120701082240-0500||VXU^V04^VXU_V04|NIST-IZ-001.00|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS\r");
		createMSH("VXU^V04^VXU_V04", "Z22", reader, sb, processingFlavorSet);
		printQueryPID(patientReported, processingFlavorSet, sb, patientMaster, sdf, 1);
		printQueryNK1(patientReported, sb, codeMap);

		int obxSetId = 0;
		int obsSubId = 0;
		{
			VaccinationMaster vaccination = vaccinationReported.getVaccination();
			Code cvxCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_CVX_CODE,
				vaccination.getVaccineCvxCode());
			if (cvxCode != null) {

				boolean originalReporter =
					vaccinationReported.getPatientReported().getTenant().equals(tenant);
				printORC(tenant, sb, vaccination, originalReporter);
				sb.append("RXA");
				// RXA-1
				sb.append("|0");
				// RXA-2
				sb.append("|1");
				// RXA-3
				sb.append("|").append(sdf.format(vaccination.getAdministeredDate()));
				// RXA-4
				sb.append("|");
				// RXA-5
				sb.append("|").append(cvxCode.getValue()).append("^").append(cvxCode.getLabel()).append("^CVX");
				if (!vaccinationReported.getVaccineNdcCode().equals("")) {
					Code ndcCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_NDC_CODE,
						vaccinationReported.getVaccineNdcCode());
					if (ndcCode != null) {
						sb.append("~").append(ndcCode.getValue()).append("^").append(ndcCode.getLabel()).append("^NDC");
					}
				}
				{
					// RXA-6
					sb.append("|");
					double adminAmount = 0.0;
					if (!vaccinationReported.getAdministeredAmount().equals("")) {
						try {
							adminAmount = Double.parseDouble(vaccinationReported.getAdministeredAmount());
						} catch (NumberFormatException nfe) {
							adminAmount = 0.0;
						}
					}
					if (adminAmount > 0) {
						if (adminAmount == 999.0) {
							sb.append("999");
						} else {
							sb.append(adminAmount);
						}
					}
					// RXA-7
					sb.append("|");
					if (adminAmount > 0) {
						sb.append("mL^milliliters^UCUM");
					}
				}
				// RXA-8
				sb.append("|");
				// RXA-9
				sb.append("|");
				{
					Code informationCode = null;
					if (vaccinationReported.getInformationSource() != null) {
						informationCode = codeMap.getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE,
							vaccinationReported.getInformationSource());
					}
					if (informationCode != null) {
						sb.append(informationCode.getValue()).append("^").append(informationCode.getLabel()).append("^NIP001");
					}
				}
				// RXA-10
				sb.append("|");
				// RXA-11
				sb.append("|");
				sb.append("^^^");
				if (vaccinationReported.getOrgLocation() == null
					|| vaccinationReported.getOrgLocation().getOrgFacilityCode() == null
					|| "".equals(vaccinationReported.getOrgLocation().getOrgFacilityCode())) {
					sb.append("AIRA");
				} else {
					sb.append(vaccinationReported.getOrgLocation().getOrgFacilityCode());
				}
				// RXA-12
				sb.append("|");
				// RXA-13
				sb.append("|");
				// RXA-14
				sb.append("|");
				// RXA-15
				sb.append("|");
				if (vaccinationReported.getLotnumber() != null) {
					sb.append(vaccinationReported.getLotnumber());
				}
				// RXA-16
				sb.append("|");
				if (vaccinationReported.getExpirationDate() != null) {
					sb.append(sdf.format(vaccinationReported.getExpirationDate()));
				}
				// RXA-17
				sb.append("|");
				sb.append(printCode(vaccinationReported.getVaccineMvxCode(),
					CodesetType.VACCINATION_MANUFACTURER_CODE, "MVX", codeMap));
				// RXA-18
				sb.append("|");
				sb.append(printCode(vaccinationReported.getRefusalReasonCode(),
					CodesetType.VACCINATION_REFUSAL, "NIP002", codeMap));
				// RXA-19
				sb.append("|");
				// RXA-20
				sb.append("|");
				if (!processingFlavorSet.contains(ProcessingFlavor.LIME)) {
					String completionStatus = vaccinationReported.getCompletionStatus();
					if (completionStatus == null || completionStatus.equals("")) {
						completionStatus = "CP";
					}
					sb.append(printCode(completionStatus, CodesetType.VACCINATION_COMPLETION, null, codeMap));
				}

				// RXA-21
				String actionCode = vaccinationReported.getActionCode();
				if (actionCode == null || actionCode.equals("")
					|| (!actionCode.equals("A") && !actionCode.equals("D"))) {
					actionCode = "A";
				}
				sb.append("|").append(vaccinationReported.getActionCode());
				sb.append("\r");
				if (vaccinationReported.getBodyRoute() != null
					&& !vaccinationReported.getBodyRoute().equals("")) {
					sb.append("RXR");
					// RXR-1
					sb.append("|");
					sb.append(printCode(vaccinationReported.getBodyRoute(), CodesetType.BODY_ROUTE, "NCIT",
						codeMap));
					// RXR-2
					sb.append("|");
					sb.append(printCode(vaccinationReported.getBodySite(), CodesetType.BODY_SITE, "HL70163",
						codeMap));
					sb.append("\r");
				}
				TestEvent testEvent = vaccinationReported.getTestEvent();
				if (testEvent != null && testEvent.getEvaluationActualList() != null) {
					for (EvaluationActual evaluationActual : testEvent.getEvaluationActualList()) {
						obsSubId++;
						{
							obxSetId++;
							String loinc = "30956-7";
							String loincLabel = "Vaccine type";
							String value = evaluationActual.getVaccineCvx();
							String valueLabel = evaluationActual.getVaccineCvx();
							String valueTable = "CVX";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
						{
							obxSetId++;
							String loinc = "59781-5";
							String loincLabel = "Dose validity";
							String value = evaluationActual.getDoseValid();
							String valueLabel = value;
							String valueTable = "99107";
							printObx(sb, obxSetId, obsSubId, loinc, loincLabel, value, valueLabel, valueTable);
						}
					}
				}

				try {
					Bundle bundle = fhirClient.search().forResource(Observation.class)
						.where(Observation.PART_OF.hasId(patientMaster.getPatientId()))
						.and(Observation.PART_OF.hasId(vaccination.getVaccinationId()))
						.returnBundle(Bundle.class).execute();
					if (bundle.hasEntry()) {
						obsSubId++;
						for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
							ObservationReported observationReported =
								observationMapper.localObjectReported((Observation) entry.getResource());
							obxSetId++;
							printObx(sb, obxSetId, obsSubId, observationReported);
						}
					}
				} catch (ResourceNotFoundException e) {
				}
			}
		}
		return sb.toString();
	}


}
