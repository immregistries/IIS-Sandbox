package org.immregistries.iis.kernal.logic;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class PatientHandlerTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Patient p= new Patient();
  PatientMaster patientMaster = new PatientMaster();
  Date date;

  OrgAccess orgAccess ;
  OrgMaster orgMaster ;

  Session dataSession=null;
  String PARAM_USERID = "TELECOM NANCY";
  String PARAM_PASSWORD = "1234";
  String PARAM_FACILITYID = "TELECOMNANCY";
  SessionFactory factory;


  @BeforeClass
  public void setUp() throws Exception {
    super.setUp();
    p.addIdentifier().setValue("Identifiant1");
    HumanName name = p.addName().setFamily("Doe").addGiven("John");
    //System.err.println(p.getNameFirstRep().getGiven().get(0).toString());
    date= new Date();
    p.setBirthDate(date);

    p.setGender(AdministrativeGender.MALE);
    p.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);

    if (factory == null) {
      factory = new AnnotationConfiguration().configure().buildSessionFactory();
    }
    dataSession =factory.openSession();

    try {
      if (orgAccess == null) {
        Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
        query.setParameter(0, PARAM_FACILITYID);
        List<OrgMaster> orgMasterList = query.list();
        if (orgMasterList.size() > 0) {
          orgMaster = orgMasterList.get(0);
        } else {
          orgMaster = new OrgMaster();
          orgMaster.setOrganizationName(PARAM_FACILITYID);
          orgAccess = new OrgAccess();
          orgAccess.setOrg(orgMaster);
          orgAccess.setAccessName(PARAM_USERID);
          orgAccess.setAccessKey(PARAM_PASSWORD);
          Transaction transaction = dataSession.beginTransaction();
          dataSession.save(orgMaster);
          dataSession.save(orgAccess);
          transaction.commit();
        }
      }

      if (orgAccess == null) {
        Query query = dataSession
            .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
        query.setParameter(0, PARAM_USERID);
        query.setParameter(1, PARAM_PASSWORD);
        query.setParameter(2, orgMaster);
        List<OrgAccess> orgAccessList = query.list();
        if (orgAccessList.size() != 0) {
          orgAccess = orgAccessList.get(0);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    dataSession= factory.openSession();


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




  public void testFindMatch() {

    Patient patient = new Patient();
    patient.addIdentifier().setValue("match");
    HumanName name = patient.addName().setFamily("Doe").addGiven("John");


    patient.setBirthDate(date);

    patient.setGender(AdministrativeGender.MALE);
    patient.addAddress().addLine("12 avenue de Nancy");
    //System.err.println(patient.getNameFirstRep().getFamily());
    //System.err.println(patient.getNameFirstRep().getGiven().get(0).toString());
    //assertEquals("1",PatientHandler.findMatch(dataSession,patient).size());
    //System.err.println(PatientHandler.findMatch(dataSession,patient).get(0).getPatientExternalLink());
    System.err.println(patient.getNameFirstRep().getFamily());
    System.err.println(patient.getNameFirstRep().getGiven().get(0).toString());
     System.err.println(patient.getBirthDate());
    System.err.println(p.getNameFirstRep().getFamily());
    System.err.println(p.getNameFirstRep().getGiven().get(0).toString());
    System.err.println(p.getBirthDate());

  }
}