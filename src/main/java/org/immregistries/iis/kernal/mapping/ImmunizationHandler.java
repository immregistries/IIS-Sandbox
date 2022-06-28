package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


public class ImmunizationHandler {


	public static VaccinationReported vaccinationReportedFromFhir(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
		vaccinationReported.setVaccinationReportedId(i.getId());
		fillVaccinationReportedFromFhirImmunization(vaccinationReported,i);
		VaccinationMaster vaccinationMaster = new VaccinationMaster();
		vaccinationMaster.setVaccinationId(vaccinationReported.getVaccinationReportedId());
		vaccinationReported.setVaccination(vaccinationMaster);
		vaccinationMasterFromFhirImmunization(vaccinationMaster, i);
		return vaccinationReported;
	}

  public static void patientReportedFromFhirImmunization(PatientReported patientReported,
      Immunization i) {
    if (i != null) {
      patientReported.setReportedDate(i.getRecorded());
      patientReported.setUpdatedDate(i.getOccurrenceDateTimeType().getValue());
      patientReported.setPatientReportedAuthority(i.getIdentifierFirstRep().getValue());
    }

  }
  /**
   * This method set the vaccinnationReported information based on the immunization information
   * @param vaccinationReported the vaccinationReported
   * @param i the Immunization resource
   */
  public static void fillVaccinationReportedFromFhirImmunization(
      VaccinationReported vaccinationReported,
		Immunization i) {
    //vaccinationReported.setVaccinationReportedId(0);
    vaccinationReported.setVaccinationReportedExternalLink(i.getId());
    if (i.getRecorded() != null) {
      vaccinationReported.setReportedDate(i.getRecorded());
    } else {
      vaccinationReported.setReportedDate(new Date());
    }
    vaccinationReported.setUpdatedDate(new Date());
    vaccinationReported.setLotnumber(i.getLotNumber());
    vaccinationReported.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
    vaccinationReported.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
    vaccinationReported.setExpirationDate(i.getExpirationDate());
    vaccinationReported.setVaccinationReportedExternalLink(i.getIdentifier().get(0).getValue());
    switch(i.getStatus()){
        case COMPLETED : vaccinationReported.setCompletionStatus("CP"); break;
        case ENTEREDINERROR : vaccinationReported.setActionCode("D"); break;
        case NOTDONE : vaccinationReported.setCompletionStatus("RE"); break; //Could also be NA or PA
      default:
        break;
    }

//    vaccinationReported.setActionCode(); TODO
    vaccinationReported.setRefusalReasonCode(i.getReasonFirstRep().getConcept().getText());
    vaccinationReported.setVaccineCvxCode(i.getVaccineCode().getCodingFirstRep().getCode());
  }

  /**
   * This method set the vaccinationMaster information based on the immunization information
   * @param vaccinationMaster the vaccinationReported
   * @param i the Immunization resource
   */
  public static void vaccinationMasterFromFhirImmunization(VaccinationMaster vaccinationMaster,
      Immunization i) {
    vaccinationMaster.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
    vaccinationMaster.setVaccineCvxCode(i.getVaccineCode().getCodingFirstRep().getCode());
  }

  /**
   * This methods create the immunization resource based on the vaccinationReported information
   *@param theRequestDetails authentification access information
   * @param vr the vaccinationReported
   * @return the Immunization resource
   */
  public static Immunization getImmunization(RequestDetails theRequestDetails,
      VaccinationReported vr) {
    Immunization i = new Immunization();
    i.setId(vr.getVaccinationReportedExternalLink());
    i.setRecorded(vr.getReportedDate());
    i.setLotNumber(vr.getLotnumber());
    i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());
    i.setDoseQuantity(new Quantity());
    i.getDoseQuantity().setValue(new BigDecimal(vr.getAdministeredAmount()));
    i.setExpirationDate(vr.getExpirationDate());
    if (vr.getActionCode().equals("D")) {
      i.setStatus(Immunization.ImmunizationStatusCodes.ENTEREDINERROR);
    } else {
      switch(vr.getCompletionStatus()) {
        case "CP" : {
          i.setStatus(Immunization.ImmunizationStatusCodes.COMPLETED);
          break;
        }
        case "NA" :
        case "PA" :
        case "RE" : {
          i.setStatus(Immunization.ImmunizationStatusCodes.NOTDONE);
          break;
        }
        case "" : {
          i.setStatus(Immunization.ImmunizationStatusCodes.NULL);
          break;
        }
        default: break;
      }
    }

    i.addReason().setConcept(
            new CodeableConcept(
                    new Coding("VXU-iis-sandbox",vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
    i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode());

    i.setPatient(new Reference("Patient/"
        + vr.getPatientReported().getPatientReportedExternalLink()));

    Location location  = LocationMapper.fhirLocation(vr.getOrgLocation());
	 i.setLocation(new Reference(location));
    Extension links = new Extension("#links");
    Extension link;
    link = new Extension();
    link.setValue(
        new StringType("/MedicationAdministration/"
            + vr.getVaccination().getVaccinationReported().getVaccinationReportedExternalLink()));
    links.addExtension(link);
    i.addExtension(links);
    return i;
  }

  /**
   * This methods is looking for matches based on the algorithm used in deduplication servlet
   * @param dataSession the Session
   * @param patientReported the patient
   * @param immunization the immunization resource
   * @return the vaccinationMaster found, null if none has been found
   */
  public static VaccinationMaster findMatch(Session dataSession, PatientReported patientReported, Immunization immunization) {
    VaccinationMaster vm = null;
    Deterministic comparer = new Deterministic();
    ComparisonResult comparison;
    org.immregistries.vaccination_deduplication.Immunization i1;
    org.immregistries.vaccination_deduplication.Immunization i2;

    i1 = new org.immregistries.vaccination_deduplication.Immunization();
    i1.setCVX(immunization.getVaccineCode().toString());
    //i1.setDate(String.valueOf(immunization.getOccurrenceDateTimeType()));
    i1.setLotNumber(immunization.getLotNumber());
    if (immunization.getPrimarySource()){
      i1.setSource(ImmunizationSource.SOURCE);
    }else {
      i1.setSource(ImmunizationSource.HISTORICAL);
    }

    {
      Query query = dataSession.createQuery(
          "from VaccinationReported where patientReported = ?");
      query.setParameter(0, patientReported);
      @SuppressWarnings("unchecked")
      List<VaccinationReported> vaccinationReportedList = query.list();

      for (VaccinationReported vaccinationReported : vaccinationReportedList){
        i2 = new org.immregistries.vaccination_deduplication.Immunization();
        i2.setCVX(vaccinationReported.getVaccineCvxCode());
        i2.setDate(vaccinationReported.getAdministeredDate());
        i2.setLotNumber(vaccinationReported.getLotnumber());
        if (immunization.getPrimarySource()){
          i2.setSource(ImmunizationSource.SOURCE);
        }else {
          i2.setSource(ImmunizationSource.HISTORICAL);
        }
        comparison = comparer.compare(i1,i2);
        if (comparison.equals(ComparisonResult.EQUAL)) {
          return vaccinationReported.getVaccination();
        }
      }
    }
    return vm;
  }

}