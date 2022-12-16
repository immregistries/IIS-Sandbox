package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.smm.tester.manager.HL7Reader;
import org.immregistries.vfa.connect.model.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public interface IIncomingMessageHandler<Organization extends IBaseResource> {
	public String process(String message, OrgAccess orgAccess);
	public String processQBP(OrgAccess orgAccess, HL7Reader reader, String messageReceived);

	public String processVXU(OrgAccess orgAccess, HL7Reader reader, String message, Organization managingOrganization);

	public PatientReported processPatient(OrgAccess orgAccess, HL7Reader reader,
													  List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSet,
													  CodeMap codeMap, boolean strictDate, PatientReported patientReported, Organization managingOrganization)
		throws ProcessingException;

	public String processORU(OrgAccess orgAccess, HL7Reader reader, String message, Organization managingOrganization);

	public int readAndCreateObservations(HL7Reader reader,
													 List<ProcessingException> processingExceptionList, PatientReported patientReported,
													 boolean strictDate, int obxCount, VaccinationReported vaccinationReported,
													 VaccinationMaster vaccination);

	@SuppressWarnings("unchecked")
	public ObservationReported readObservations(HL7Reader reader,
															  List<ProcessingException> processingExceptionList, PatientReported patientReported,
															  boolean strictDate, int obxCount, VaccinationReported vaccinationReported,
															  VaccinationMaster vaccination, String identifierCode, String valueCode);

	public void verifyNoErrors(List<ProcessingException> processingExceptionList)
		throws ProcessingException;

	public boolean hasErrors(List<ProcessingException> processingExceptionList);
	public void recordMessageReceived(String message, PatientReported patientReported,
												 String messageResponse, String categoryRequest, String categoryResponse,
												 OrgMaster orgMaster);

	public String buildRSP(HL7Reader reader, String messageRecieved, PatientReported patientReported,
								  OrgAccess orgAccess, List<PatientReported> patientReportedPossibleList,
								  List<ProcessingException> processingExceptionList);

	public String buildVxu(VaccinationReported vaccinationReported, OrgAccess orgAccess);

	public List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient);

	public void printQueryNK1(PatientReported patientReported, StringBuilder sb, CodeMap codeMap);

	public void printQueryPID(PatientReported patientReported,
									  Set<ProcessingFlavor> processingFlavorSet, StringBuilder sb, PatientMaster patient,
									  SimpleDateFormat sdf, int pidCount);

	public void printORC(OrgAccess orgAccess, StringBuilder sb, VaccinationMaster vaccination,
								VaccinationReported vaccinationReported, boolean originalReporter);

	public List<ForecastActual> doForecast(PatientMaster patient, PatientReported patientReported,
														CodeMap codeMap, List<VaccinationMaster> vaccinationMasterList, OrgAccess orgAccess);

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

	public void printERRSegment(ProcessingException e, StringBuilder sb);

	public void createMSH(String messageType, String profileId, HL7Reader reader, StringBuilder sb,
								 Set<ProcessingFlavor> processingFlavorSet);

	public Date parseDateWarn(String dateString, String errorMessage, String segmentId,
									  int segmentRepeat, int fieldPosition, boolean strict,
									  List<ProcessingException> processingExceptionList);

	public Date parseDateError(String dateString, String errorMessage, String segmentId,
										int segmentRepeat, int fieldPosition, boolean strict) throws ProcessingException;

	public Date parseDateInternal(String dateString, boolean strict) throws ParseException;

	public String generatePatientExternalLink();

	public String generateId();

}
