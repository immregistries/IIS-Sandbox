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
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class RestfulPatientResourceProviderTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Patient p= new Patient();
  PatientMaster patientMaster = new PatientMaster();
  //SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
  //Session dataSession = factory.openSession();
  OrgAccess orgAccess ;
  OrgMaster orgMaster ;

  Session dataSession=null;
  String PARAM_USERID = "TELECOM NANCY";
  String PARAM_PASSWORD = "1234";
  String PARAM_FACILITYID = "TELECOMNANCY";
  SessionFactory factory;



  public void setUp() throws Exception {
    super.setUp();
    p.addIdentifier().setValue("Identifiant1");
    HumanName name = p.addName().setFamily("Doe").addGiven("John");

    System.err.println((p.getNameFirstRep().getGiven().get(0)));
    Date date= new Date();
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

    Query query = dataSession.createQuery(
        "from PatientReported where patientNameFirst = ?");
    query.setParameter(0,"John");
    List<PatientReported> patientReportedList= query.list();
    System.err.println(patientReportedList.get(0).getPatientNameFirst());



    FHIRHandler fhirHandler = new FHIRHandler(dataSession);
    patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,p,null);

  }

  public void tearDown() throws Exception {
    patientReported =null;
    p = null;
    patientMaster=null;
    dataSession=null;
  }
  

  public void testGetPatientById() {
    assertEquals("Identifiant1",RestfulPatientResourceProvider.getPatientById("Identifiant1",dataSession,orgAccess).getIdentifier().get(0).getValue());

  }

  public void testUpdatePatient() throws Exception {
    p.setGender(AdministrativeGender.FEMALE);
    FHIRHandler fhirHandler = new FHIRHandler(dataSession);
    patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,p,null);

    assertEquals("F",patientReported.getPatientSex());
  }

  public void testDeletePatientById() {
  }
}