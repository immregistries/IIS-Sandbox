package org.immregistries.iis.kernal.logic;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.smm.tester.manager.HL7Reader;

import java.util.List;
import java.util.Random;
import java.util.Set;

public interface IIncomingMessageHandler {
	double MINIMAL_MATCHING_SCORE = 0.9;

	String PATIENT_MIDDLE_NAME_MULTI = "Multi";
	String QBP_Z34 = "Z34";
	String QBP_Z44 = "Z44";
	String RSP_Z42_MATCH_WITH_FORECAST = "Z42";
	String RSP_Z32_MATCH = "Z32";
	String RSP_Z31_MULTIPLE_MATCH = "Z31";
	String RSP_Z33_NO_MATCH = "Z33";
	String Z23_ACKNOWLEDGEMENT = "Z23";
	String VXU_Z22 = "Z22^CDCPHINVS";
	String Z22_ACK = "Z23^CDCPHINVS";
	String ADVANCED_ACK = "ADVANCED_ACK";
	String Z22_ADVANCED_VXU = VXU_Z22 + "~" + ADVANCED_ACK;
	String Z23_ADVANCED_ACKNOWLEDGEMENT = "Z23^CDCPHINVS" + "~" + ADVANCED_ACK;
	String QUERY_OK = "OK";
	// TODO:
	// Organize logic classes, need to have access classes for every object, maybe a new Access
	// package?
	// Look at names of database fields, make more consistent
	String QUERY_NOT_FOUND = "NF";
	String QUERY_TOO_MANY = "TM";
	String QUERY_APPLICATION_ERROR = "AE";
	Random random = new Random();

	String process(String message, Tenant tenant, String facilityName);

	List<VaccinationMaster> getVaccinationMasterList(PatientMaster patient);

	String buildAck(HL7Reader reader, List<ProcessingException> processingExceptionList, Set<ProcessingFlavor> processingFlavorSetå);

}
