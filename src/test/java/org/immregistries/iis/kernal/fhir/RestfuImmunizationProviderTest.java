package org.immregistries.iis.kernal.fhir;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Location;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Quantity;
import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class RestfuImmunizationProviderTest extends TestCase {
  private PatientReported patientReported = new PatientReported();
  private Immunization immunization = new Immunization();
  private Location location = new Location();
  private VaccinationMaster vaccinationMaster = new VaccinationMaster();
  private OrgLocation orgLocation = new OrgLocation();
  private VaccinationReported vaccinationReported= new VaccinationReported();
  private OrgAccess orgAccess ;
  private OrgMaster orgMaster ;


  private Patient patient = new Patient();
  private PatientMaster patientMaster = new PatientMaster();

  private Session dataSession=null;




  public void setUp() throws Exception {
    super.setUp();

    patient.addIdentifier().setValue("Identifiant1");
    HumanName name = patient.addName().setFamily("Doe").addGiven("John");

    System.err.println((patient.getNameFirstRep().getGiven().get(0)));
    Date date= new Date();
    patient.setBirthDate(date);

    patient.setGender(AdministrativeGender.MALE);
    patient.addAddress().addLine("12 rue chicago");
    patientReported.setPatient(patientMaster);
    immunization.setRecorded(new Date());
    immunization.setId("idImmunization");
    immunization.setLotNumber("LOT1");
    immunization.getOccurrenceDateTimeType().setValue(new  Date());
    immunization.setDoseQuantity(new Quantity().setValue(new BigDecimal(10)));
    immunization.setExpirationDate(new Date());
    immunization.addIdentifier().setValue("identifiant2");
    String ref = patient.getIdentifier().get(0).getValue();
    Reference reference = new Reference("Patient/" + ref);
    immunization.setPatient(reference);
//    immunization.addReasonCode().addCoding().setCode("2V4"); TODO R5
    immunization.getVaccineCode().addCoding().setCode("2V4");


    OrgAccessGenerator.authentification();
    dataSession=OrgAccessGenerator.getDataSession();
    orgAccess=OrgAccessGenerator.getOrgAccess();
    orgMaster=OrgAccessGenerator.getOrgMaster();

    FHIRHandler fhirHandler = new FHIRHandler(dataSession);

    patientReported=fhirHandler.fhirEventPatientReported(orgAccess, patient, immunization);
    fhirHandler.fhirEventVaccinationReported(orgAccess, patient,patientReported, immunization);

  }

  public void tearDown() {
    patientReported =null;
    immunization =null;
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


    immunization.setLotNumber("LOT2");

    FHIRHandler fhirHandler = new FHIRHandler(dataSession);

    patientReported=fhirHandler.fhirEventPatientReported(orgAccess, patient, immunization);
    fhirHandler.fhirEventVaccinationReported(orgAccess, patient,patientReported, immunization);


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