package org.immregistries.iis.kernal.fhir;

import java.util.Date;
import java.util.List;
import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public class RestfulPatientResourceProviderTest extends TestCase {
  private PatientReported patientReported = new PatientReported();
  private Patient patient = new Patient();
  private PatientMaster patientMaster = new PatientMaster();
  //SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
  //Session dataSession = factory.openSession();
  private OrgAccess orgAccess ;
  private OrgMaster orgMaster ;

  private Session dataSession=null;



  public void setUp() throws Exception {
    super.setUp();
    patient.addIdentifier().setValue("Identifiant1");
    HumanName name = patient.addName().setFamily("Doe").addGiven("John");


    Date date= new Date();
    patient.setBirthDate(date);

    patient.setGender(AdministrativeGender.MALE);
    patient.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);
    OrgAccessGenerator.authentification();

    dataSession=OrgAccessGenerator.getDataSession();
    orgAccess=OrgAccessGenerator.getOrgAccess();
    orgMaster=OrgAccessGenerator.getOrgMaster();
    FHIRHandler fhirHandler = new FHIRHandler(dataSession);
    patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess, patient,null);


  }

  public void tearDown() throws Exception {
    patientReported =null;
    patient = null;
    patientMaster=null;
    dataSession=null;

  }
  

  public void testGetPatientById() {
    assertEquals("Identifiant1",RestfulPatientResourceProvider.getPatientById("Identifiant1",dataSession,orgAccess).getIdentifier().get(0).getValue());

  }

  public void testUpdatePatient() throws Exception {
    patient.setGender(AdministrativeGender.FEMALE);
    FHIRHandler fhirHandler = new FHIRHandler(dataSession);
    patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess, patient,null);

    assertEquals("F",patientReported.getPatientSex());
  }


}