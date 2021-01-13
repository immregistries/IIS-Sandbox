package org.immregistries.iis.kernal.logic;

import java.util.Date;
import junit.framework.TestCase;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public class PatientHandlerTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Patient p= new Patient();
  PatientMaster patientMaster = new PatientMaster();
  public void setUp() throws Exception {
    super.setUp();
    p.addIdentifier().setValue("Identifiant1");
    HumanName name = p.addName().setFamily("Doe");
    Date date= new Date();
    p.setBirthDate(date);

    p.setGender(AdministrativeGender.MALE);
    p.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);

  }

  public void tearDown() throws Exception {
    patientReported =null;
    p = null;
    patientMaster=null;
  }

  public void testPatientReportedFromFhirPatient() {
    PatientHandler.patientReportedFromFhirPatient(patientReported,p);
    assertEquals("Identifiant1", patientReported.getPatientReportedExternalLink());
    assertEquals("Doe",patientReported.getPatientNameLast());
    assertFalse(patientReported.getPatientBirthDate()==null);
    assertEquals("M", patientReported.getPatientSex());
    assertEquals("12 rue chicago",patientReported.getPatientAddressLine1());

    
  }

  public void testPatientReportedToFhirPatient() {
  }

  public void testGetPatient() {
  }

  public void testFindPossibleMatch() {
    //need database
  }

  public void testFindMatch() {
    // need database
  }
}