package org.immregistries.iis.kernal.logic;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.*;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

import java.util.Date;
import java.util.List;


public class FHIRHandler extends IncomingMessageHandler {


  public FHIRHandler(Session dataSession) {
    super(dataSession);
  }

  public void processFhirEvent(OrgAccess orgAccess, Patient patient, Immunization immunization) throws HibernateException, InvalidRequestException {
    PatientReported patientReported = fhirEventPatientReported(orgAccess,patient,immunization);
    fhirEventVaccinationReported(orgAccess,patient,patientReported,immunization);
  }

  /**
   * This methods is collecting the patient information to add or update a patient Reported
   * @param orgAccess the orgAccess
   * @param patient the patient resource
   * @param immunization the immunization resource
   * @return the patientReported added or updated
   * @throws Exception
   */
  public PatientReported fhirEventPatientReported(OrgAccess orgAccess, Patient patient, Immunization immunization) throws InvalidRequestException, HibernateException {
    PatientMaster patientMaster = null;
    PatientReported patientReported = null;
    String patientReportedExternalLink = patient.getIdentifier().get(0).getValue();

    boolean patientAlreadyExists = false;
    int levelConfidence=0;

    if (StringUtils.isEmpty(patientReportedExternalLink)) {
      throw new InvalidRequestException("Patient external link must be indicated");
    }

    { //EMPI SYSTEM LOGIC TO FIND DUPLICATES
      Query query = dataSession.createQuery(
          "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, patientReportedExternalLink);
      @SuppressWarnings("unchecked")
      List<PatientReported> patientReportedList = query.list();
      if (!patientReportedList.isEmpty()) {
        // System.err.println("Patient already exists");
        // Get patient master and reported
        patientReported = patientReportedList.get(0);
        PatientHandler.patientReportedFromFhirPatient(patientReported,patient);
        patientMaster = patientReported.getPatient();

      } else { //EMPI Search matches with firstname, lastname and birthday
        List<PatientMaster> patientMasterList = PatientHandler.findMatch(dataSession,patient);
        if(!patientMasterList.isEmpty()){
          // System.err.println("patient has a match");
          // Create new patient reported and get existing patient master
          patientAlreadyExists = true;
          patientMaster = patientMasterList.get(0);
          levelConfidence=2;
        } else if(!PatientHandler.findPossibleMatch(dataSession,patient).isEmpty()){
          // System.err.println("patient has a possible match ");
          // Found an existing patient with same firstname and lastname
          patientAlreadyExists=true;
          patientMasterList=PatientHandler.findPossibleMatch(dataSession,patient);
          patientMaster=patientMasterList.get(0);
          levelConfidence=1;
        } else {
          // Create new patient master and patient reported
          // System.err.println("patient has no match ");
          patientMaster = new PatientMaster();
          patientMaster.setOrgMaster(orgAccess.getOrg());
        }
        patientReported = new PatientReported();
        patientReported.setPatient(patientMaster);
        PatientHandler.patientReportedFromFhirPatient(patientReported, patient);
        patientReported.setOrgReported(orgAccess.getOrg());
        patientReported.setUpdatedDate(new Date());
      }
    }

    {
      Transaction transaction = dataSession.beginTransaction();


      dataSession.saveOrUpdate(patientMaster);
      dataSession.saveOrUpdate(patientReported);
      // System.err.println("patient Created");
      if(patientAlreadyExists) {
        // System.err.println("Creating patientlink");
        PatientLink pl = new PatientLink();
        pl.setLevelConfidence(levelConfidence);
        pl.setPatientMaster(patientMaster);
        pl.setPatientReported(patientReported);
        dataSession.saveOrUpdate(pl);
      }
      transaction.commit();
    }

    return patientReported;
  }

  /**
   * This methods is collecting the immunization information to add or update a vaccinationReported
   * @param orgAccess the orgAccess
   * @param patient the patient resource
   * @param patientReported the patientReported
   * @param immunization the immunization resource
   * @return the vaccinationReported added or updated
   * @throws Exception
   */
  public VaccinationReported fhirEventVaccinationReported(OrgAccess orgAccess, Patient patient,PatientReported patientReported, Immunization immunization) throws HibernateException {
    VaccinationMaster vaccinationMaster = null;
    VaccinationReported vaccinationReported = null;

    vaccinationReported = new VaccinationReported();
    vaccinationReported.setPatientReported(patientReported);
    ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,immunization);
    {
      Query query = dataSession.createQuery(
          "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
      query.setParameter(0, patientReported);
      query.setParameter(1, immunization.getId());
      @SuppressWarnings("unchecked")
      List<VaccinationReported> vaccinationReportedList = query.list();
      if (!vaccinationReportedList.isEmpty()) { // if external link found
        // System.err.println("Immunization already exists");
        vaccinationMaster = vaccinationReportedList.get(0).getVaccination();
      } else {
        vaccinationMaster = ImmunizationHandler.findMatch(dataSession, patientReported, immunization);
      }
    }
    if (vaccinationMaster == null){
      vaccinationMaster = new VaccinationMaster();
      vaccinationMaster.setPatient(patientReported.getPatient());
      vaccinationMaster.setVaccinationReported(vaccinationReported);
      vaccinationMaster.setPatient(patientReported.getPatient());
      ImmunizationHandler.vaccinationMasterFromFhirImmunization(vaccinationMaster,immunization);
    }
    vaccinationReported.setVaccination(vaccinationMaster);
    if (vaccinationReported.getUpdatedDate().before(patient.getBirthDate())) {
      throw new InvalidRequestException("Vaccination is reported as having been administered before the patient was born");
    }

    // OrgLocation
    String administeredAtLocation = immunization.getLocationTarget().getId();
    if (StringUtils.isNotEmpty(administeredAtLocation)) {
      Query query = dataSession.createQuery(
          "from OrgLocation where orgMaster = :orgMaster and orgFacilityCode = :orgFacilityCode");
      query.setParameter("orgMaster", orgAccess.getOrg());
      query.setParameter("orgFacilityCode", administeredAtLocation);
      @SuppressWarnings("unchecked")
      List<OrgLocation> orgMasterList = query.list();
      OrgLocation orgLocation = null;
      if (!orgMasterList.isEmpty()) {
        orgLocation = orgMasterList.get(0);
      } else {
        orgLocation = new OrgLocation();
        ImmunizationHandler.orgLocationFromFhirImmunization(orgLocation, immunization);
        orgLocation.setOrgMaster(orgAccess.getOrg());
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(orgLocation);
        transaction.commit();
      }
      vaccinationReported.setOrgLocation(orgLocation);
    }


    Transaction transaction = dataSession.beginTransaction();
    dataSession.saveOrUpdate(vaccinationMaster);
    dataSession.saveOrUpdate(vaccinationReported);
    transaction.commit();
    return vaccinationReported;
  }

  /**
   * This method deletes the vacinationReported from the database with the provided id
   * @param orgAccess the orgAcess of the organization
   * @param id the id of the vaccinationReported to be deleted
   */
  public void fhirEventVaccinationDeleted(OrgAccess orgAccess, String id) {
    VaccinationReported vr = new VaccinationReported();
    VaccinationMaster vm = new VaccinationMaster();

    Query query = dataSession.createQuery(
        "from  VaccinationReported where vaccinationReportedExternalLink = ?");
    query.setParameter(0, id);
    @SuppressWarnings("unchecked")
    List<VaccinationReported> vaccinationReportedList = query.list();
    if (!vaccinationReportedList.isEmpty()) {
      vr = vaccinationReportedList.get(0);
      vm = vr.getVaccination();
    }
    Transaction transaction = dataSession.beginTransaction();

    dataSession.delete(vr);
    dataSession.delete(vm);
    transaction.commit();
  }

}