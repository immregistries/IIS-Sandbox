package org.immregistries.iis.kernal.logic;

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
import org.immregistries.iis.kernal.fhir.OrgAccessGenerator;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

public class PatientHandlerTest extends TestCase {
  private PatientReported patientReported = new PatientReported();
  private Patient patient = new Patient();
  private PatientMaster patientMaster = new PatientMaster();
  private Date date;

  private OrgAccess orgAccess ;
  private OrgMaster orgMaster ;

  private Session dataSession=null;



  public void setUp() throws Exception {
    super.setUp();
    patient.addIdentifier().setValue("Identifiant1");
    HumanName name = patient.addName().setFamily("Doe").addGiven("John");
    //System.err.println(p.getNameFirstRep().getGiven().get(0).toString());
    date= new Date();
    patient.setBirthDate(date);

    patient.setGender(AdministrativeGender.MALE);
    patient.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);

    OrgAccessGenerator.authentification();
    dataSession=OrgAccessGenerator.getDataSession();
    orgAccess=OrgAccessGenerator.getOrgAccess();
    orgMaster=OrgAccessGenerator.getOrgMaster();
    //System.err.println("datasession patient handler test " + dataSession!=null);

  }

  public void tearDown() throws Exception {
    patientReported =null;
    patient = null;
    patientMaster=null;
    dataSession=null;
  }

  public void testpatientReportedFromFhir() {
    PatientHandler.patientReportedFromFhir(patientReported, patient);
    assertEquals("Identifiant1", patientReported.getPatientReportedExternalLink());
    assertEquals("Doe",patientReported.getPatientNameLast());
    assertFalse(patientReported.getPatientBirthDate()==null);
    assertEquals("M", patientReported.getPatientSex());
    assertEquals("12 rue chicago",patientReported.getPatientAddressLine1());
    assertEquals("John",patientReported.getPatientNameFirst());



    
  }




  public void testFindPossibleMatch() throws Exception {
    //to be reviewed
    FHIRHandler fhirHandler = new FHIRHandler(dataSession);
//    System.err.println("datasession patient handler test " + dataSession!=null);

    fhirHandler.fhirEventPatientReported(orgAccess, patient,null);
    /*List<PatientMaster> matches;
    Query queryBigMatch = dataSession.createQuery(
        "from PatientMaster where patientNameLast = ? and patientNameFirst= ? ");
    queryBigMatch.setParameter(0, p.getNameFirstRep().getFamily());
    queryBigMatch.setParameter(1, p.getNameFirstRep().getGiven().get(0).toString());

    matches = queryBigMatch.list();
    System.err.println(matches.size());*/


    Patient patient = new Patient();
    patient.addIdentifier().setValue("match");
    HumanName name = patient.addName().setFamily("Doe").addGiven("John");


    patient.setBirthDate(date);

    patient.setGender(AdministrativeGender.MALE);
    patient.addAddress().addLine("12 avenue de Nancy");
    assertTrue(PatientHandler.findPossibleMatch(dataSession,patient).size()>0);


  }
}