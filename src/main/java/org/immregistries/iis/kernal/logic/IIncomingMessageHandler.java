package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public interface IIncomingMessageHandler<Organization extends IBaseResource> {
	public String process(String message, OrgMaster orgMaster);


	public String buildVxu(VaccinationReported vaccinationReported, OrgMaster orgMaster);

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
