package org.immregistries.iis.kernal.logic;

import java.util.Date;
import junit.framework.TestCase;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class PatientHandlerTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Patient p= new Patient();
  PatientMaster patientMaster = new PatientMaster();
  @BeforeClass
  public void setUp() throws Exception {
    super.setUp();
    p.addIdentifier().setValue("Identifiant1");
    HumanName name = p.addName().setFamily("Doe").addGiven("John");
    System.err.println(p.getNameFirstRep().getGiven().get(0));
    System.err.println(p.getNameFirstRep().getFamily());
    Date date= new Date();
    p.setBirthDate(date);

    p.setGender(AdministrativeGender.MALE);
    p.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);

  }
  @AfterClass
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
    assertEquals("John",patientReported.getPatientNameFirst());

    
  }


  public void testFindPossibleMatch() {
    //need database
  }

  public void testFindMatch() {
    // need database
  }
}