package org.immregistries.iis.kernal.logic;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.model.*;

import java.util.Date;
import java.util.List;

public class FHIRHandler extends IncomingMessageHandler {

    public FHIRHandler(Session dataSession) {
        super(dataSession);
    }

    public void processFIHR_Event(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        PatientReported patientReported = FIHR_EventPatientReported(orgAccess,patient,immunization);
        //VaccinationReported vaccinationReported = FHIR_EventVaccinationReported(orgAccess,patient,immunization);
        VaccinationReported vaccinationReported = null;
        VaccinationMaster vaccination = null;
        Date administrationDate = null;
        String vaccinationReportedExternalLink = immunization.getId();

        {
            Query query = dataSession.createQuery(
                    "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
            query.setParameter(0, patientReported);
            query.setParameter(1, vaccinationReportedExternalLink);
            List<VaccinationReported> vaccinationReportedList = query.list();
            if (vaccinationReportedList.size() > 0) {
                vaccinationReported = vaccinationReportedList.get(0);
                vaccination = vaccinationReported.getVaccination();
            }
        }
        if (vaccinationReported == null) {
            vaccination = new VaccinationMaster();
            vaccinationReported = new VaccinationReported();
            vaccinationReported.setVaccination(vaccination);
            vaccination.setVaccinationReported(vaccinationReported);
            vaccinationReported.setReportedDate(new Date());
        }
        if (vaccinationReported.getAdministeredDate().before(patientReported.getPatientBirthDate())) {
            throw new Exception(
                    "Vaccination is reported as having been administered before the patient was born");
        }

        {
            String administeredAtLocation = immunization.getLocationTarget().getId();
            if (StringUtils.isNotEmpty(administeredAtLocation)) {
                Query query = dataSession.createQuery(
                        "from OrgLocation where orgMaster = :orgMaster and orgFacilityCode = :orgFacilityCode");
                query.setParameter("orgMaster", orgAccess.getOrg());
                query.setParameter("orgFacilityCode", administeredAtLocation);
                List<OrgLocation> orgMasterList = query.list();
                OrgLocation orgLocation = null;
                if (orgMasterList.size() > 0) {
                    orgLocation = orgMasterList.get(0);
                }

                if (orgLocation == null) {
                    orgLocation = new OrgLocation();
                    ImmunizationHandler.orgLocationFromFhirImmunization(orgLocation, immunization);
                    orgLocation.setOrgMaster(orgAccess.getOrg());
                    Transaction transaction = dataSession.beginTransaction();
                    dataSession.save(orgLocation);
                    transaction.commit();
                }
                vaccinationReported.setOrgLocation(orgLocation);
            }

        }
        {
            Transaction transaction = dataSession.beginTransaction();
            dataSession.saveOrUpdate(vaccination);
            dataSession.saveOrUpdate(vaccinationReported);
            vaccination.setVaccinationReported(vaccinationReported);
            dataSession.saveOrUpdate(vaccination);
            transaction.commit();
        }
    }

    public PatientReported FIHR_EventPatientReported(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        PatientMaster patientMaster = null;
        PatientReported patientReported = null;

        //Initialising and linking the objects
        /*PatientMaster patientMaster = new PatientMaster();
        PatientReported patientReported = new PatientReported();
        VaccinationMaster vaccinationMaster = new VaccinationMaster();
        VaccinationReported vaccinationReported = new VaccinationReported();
        patientReported.setPatient(patientMaster);
        vaccinationMaster.setPatient(patientMaster);
        vaccinationReported.setPatientReported(patientReported);
        vaccinationReported.setVaccination(vaccinationMaster);
        patientMaster.setOrgMaster(orgAccess.getOrg());
        patientReported.setOrgReported(orgAccess.getOrg());*/




        /*patientReported = null;
        patient = null;*/
        /*PatientHandler.patientReportedFromFhirPatient(patientReported, patient);
        ImmunizationHandler.patientReportedFromFhirImmunization(patientReported,immunization);
        ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,immunization);*/

        String patientReportedExternalLink = patient.getId();
        String patientReportedAuthority = immunization.getIdentifierFirstRep().getValue();
        //String patientReportedType = patientReported.getPatientReportedType();
        if (StringUtils.isEmpty(patientReportedExternalLink)) {
            throw new Exception("Patient external link must be indicated");
        }
        {
            Query query = dataSession.createQuery(
                    "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
            query.setParameter(0, orgAccess.getOrg());
            query.setParameter(1, patientReportedExternalLink);
            List<PatientReported> patientReportedList = query.list();
            if (patientReportedList.size() > 0) {
                patientReported = patientReportedList.get(0);
                patientMaster = patientReported.getPatient();
            }
        }
        if (patientReported == null) {
            patientMaster = new PatientMaster();
            patientReported = new PatientReported();
            patientReported.setPatient(patientMaster);
            PatientHandler.patientReportedFromFhirPatient(patientReported, patient);
            ImmunizationHandler.patientReportedFromFhirImmunization(patientReported,immunization);
        }

        {
            Transaction transaction = dataSession.beginTransaction();
            dataSession.saveOrUpdate(patientMaster);
            dataSession.saveOrUpdate(patientReported);
            transaction.commit();
        }
        return patientReported;
    }

    public VaccinationReported FHIR_EventVaccinationReported(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        VaccinationReported vaccinationReported = null;
        return vaccinationReported;
    }
}
