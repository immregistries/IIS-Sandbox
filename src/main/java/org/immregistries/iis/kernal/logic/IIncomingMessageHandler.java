package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;

import java.util.*;

public interface IIncomingMessageHandler {
	public String process(String message, Tenant tenant, String facilityName);

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient);

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
								String loincLabel, String value);

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId,
								ObservationReported ob);

	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
								String loincLabel, String value, String valueLabel, String valueTable);


	public void printObx(StringBuilder sb, int obxSetId, int obsSubId, String loinc,
								String loincLabel, Date value);

	public String printCode(String value, CodesetType codesetType, String tableName,
									CodeMap codeMap);

	public String buildAck(HL7Reader reader, List<ProcessingException> processingExceptionList);



}
