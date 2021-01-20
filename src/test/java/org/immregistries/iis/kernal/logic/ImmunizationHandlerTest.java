package org.immregistries.iis.kernal.logic;

import java.math.BigDecimal;
import java.util.Date;
import junit.framework.TestCase;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public class ImmunizationHandlerTest extends TestCase {
  PatientReported patientReported = new PatientReported();
  Immunization i = new Immunization();
  Location location = new Location();
  VaccinationMaster vaccinationMaster = new VaccinationMaster();
  OrgLocation orgLocation = new OrgLocation();
  VaccinationReported vaccinationReported= new VaccinationReported();
  public void setUp() throws Exception {
    super.setUp();
    i.setRecorded(new Date());
    i.setId("idImmunization");
    i.setLotNumber("LOT1");
    i.getOccurrenceDateTimeType().setValue(new  Date());
    i.setDoseQuantity(new Quantity().setValue(new BigDecimal(10)));
    i.setExpirationDate(new Date());
    i.addIdentifier().setValue("identifiant1");



  }

  public void tearDown() throws Exception {
    patientReported =null;
    i =null;
    location=null;
    vaccinationMaster=null;
    orgLocation=null;
    vaccinationReported=null;

  }
  public void testPatientReportedFromFhirImmunization() {
    ImmunizationHandler.patientReportedFromFhirImmunization(patientReported,i);
    assertTrue(patientReported.getReportedDate()!=null);

  }

  public void testVaccinationReportedFromFhirImmunization() {
    ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,i);
    assertTrue(vaccinationReported.getReportedDate()!=null);
    assertTrue(vaccinationReported.getUpdatedDate()!=null);
    assertEquals(vaccinationReported.getLotnumber(),"LOT1");
    assertTrue(vaccinationReported.getAdministeredDate()!=null);
    assertEquals(vaccinationReported.getAdministeredAmount(),new BigDecimal(10).toString());
    assertEquals("identifiant1",vaccinationReported.getVaccinationReportedExternalLink());


  }

  public void testVaccinationMasterFromFhirImmunization() {
  }

  public void testOrgLocationFromFhirImmunization() {
  }


}