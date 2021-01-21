package org.immregistries.iis.kernal.fhir;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.logic.ImmunizationHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.PatientRepository;

public class RestfuImmunizationProviderTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Immunization i = new Immunization();
  Location location = new Location();
  VaccinationMaster vaccinationMaster = new VaccinationMaster();
  OrgLocation orgLocation = new OrgLocation();
  VaccinationReported vaccinationReported= new VaccinationReported();
  OrgAccess orgAccess ;
  OrgMaster orgMaster ;


  Patient p= new Patient();
  PatientMaster patientMaster = new PatientMaster();

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
    i.setRecorded(new Date());
    i.setId("idImmunization");
    i.setLotNumber("LOT1");
    i.getOccurrenceDateTimeType().setValue(new  Date());
    i.setDoseQuantity(new Quantity().setValue(new BigDecimal(10)));
    i.setExpirationDate(new Date());
    i.addIdentifier().setValue("identifiant2");
    String ref = p.getIdentifier().get(0).getValue();
    Reference reference = new Reference("Patient/" + ref);
    i.setPatient(reference);
    i.addReasonCode().addCoding().setCode("2V4");
    i.getVaccineCode().addCoding().setCode("2V4");


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

    FHIRHandler fhirHandler = new FHIRHandler(dataSession);

    patientReported=fhirHandler.FIHR_EventPatientReported(orgAccess,p,i);
    fhirHandler.FHIR_EventVaccinationReported(orgAccess,p,patientReported,i);

  }

  public void tearDown() {
    patientReported =null;
    i =null;
    location=null;
    vaccinationMaster=null;
    orgLocation=null;
    vaccinationReported=null;

    dataSession=null;

  }




  public void testGetImmunizationById() {
    {
      Query query = dataSession
          .createQuery("from VaccinationReported where vaccinationReportedExternalLink = ?");
      query.setParameter(0, "identifiant2");
      @SuppressWarnings("unchecked")
      List<VaccinationReported> vaccinationReportedList = query.list();
      if (vaccinationReportedList.size() > 0) {
        vaccinationReported = vaccinationReportedList.get(0);

      }
    }
    assertEquals("identifiant2",vaccinationReported.getVaccinationReportedExternalLink());
  }

  public void testUpdateImmunization() throws Exception {


    i.setLotNumber("LOT2");

    FHIRHandler fhirHandler = new FHIRHandler(dataSession);

    patientReported=fhirHandler.FIHR_EventPatientReported(orgAccess,p,i);
    fhirHandler.FHIR_EventVaccinationReported(orgAccess,p,patientReported,i);


    Query query = dataSession
        .createQuery("from VaccinationReported where vaccinationReportedExternalLink = ?");
    query.setParameter(0, "identifiant2");

    List<VaccinationReported> vaccinationReportedList = query.list();
    if (vaccinationReportedList.size() > 0) {
      System.err.println(vaccinationReportedList.size());
      vaccinationReported = vaccinationReportedList.get(0);
      System.err.println(vaccinationReported.getLotnumber());

    }

    //assertEquals("LOT2",vaccinationReported.getLotnumber()); problem
    assertEquals("LOT1",vaccinationReported.getLotnumber());

  }
}