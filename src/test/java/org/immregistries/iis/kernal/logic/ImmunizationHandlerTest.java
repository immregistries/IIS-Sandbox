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
  private PatientReported patientReported = new PatientReported();
  private Immunization immunization = new Immunization();
  private Location location = new Location();
  private VaccinationMaster vaccinationMaster = new VaccinationMaster();
  private OrgLocation orgLocation = new OrgLocation();
  private VaccinationReported vaccinationReported= new VaccinationReported();
  public void setUp() throws Exception {
    super.setUp();
    immunization.setRecorded(new Date());
    immunization.setId("idImmunization");
    immunization.setLotNumber("LOT1");
    immunization.getOccurrenceDateTimeType().setValue(new  Date());
    immunization.setDoseQuantity(new Quantity().setValue(new BigDecimal(10)));
    immunization.setExpirationDate(new Date());
    immunization.addIdentifier().setValue("testImmunization");



  }

  public void tearDown() throws Exception {
    patientReported =null;
    immunization =null;
    location=null;
    vaccinationMaster=null;
    orgLocation=null;
    vaccinationReported=null;

  }
  public void testPatientReportedFromFhirImmunization() {
    ImmunizationHandler.patientReportedFromFhirImmunization(patientReported,immunization);
    assertTrue(patientReported.getReportedDate()!=null);

  }

  public void testVaccinationReportedFromFhirImmunization() {
    ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,immunization);
    assertTrue(vaccinationReported.getReportedDate()!=null);
    assertTrue(vaccinationReported.getUpdatedDate()!=null);
    assertEquals(vaccinationReported.getLotnumber(),"LOT1");
    assertTrue(vaccinationReported.getAdministeredDate()!=null);
    assertEquals(vaccinationReported.getAdministeredAmount(),new BigDecimal(10).toString());
    assertEquals("testImmunization",vaccinationReported.getVaccinationReportedExternalLink());


  }

  public void testVaccinationMasterFromFhirImmunization() {
  }

  public void testOrgLocationFromFhirImmunization() {
  }


}