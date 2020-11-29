package org.immregistries.iis.kernal.logic;

import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
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
        VaccinationReported vaccinationReported = FHIR_EventVaccinationReported(orgAccess,patient,patientReported,immunization);
    }

    public PatientReported FIHR_EventPatientReported(OrgAccess orgAccess, Patient patient, Immunization immunization) throws Exception {
        PatientMaster patientMaster = null;
        PatientReported patientReported = null;
        String patientReportedExternalLink = patient.getIdentifier().get(0).getValue();

        if(immunization != null){
            String patientReportedAuthority = immunization.getIdentifierFirstRep().getValue();
        }

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
                //patientReported = patientReportedList.get(0);
                //PatientHandler.patientReportedFromFhirPatient(patientReported,patient);
                patientMaster = patientReportedList.get(0).getPatient();

            } else {
				patientMaster = new PatientMaster();
				patientMaster.setOrgMaster(orgAccess.getOrg());
			}
        }
		patientReported = new PatientReported();
		patientReported.setPatient(patientMaster);
		PatientHandler.patientReportedFromFhirPatient(patientReported, patient);
        patientReported.setOrgReported(orgAccess.getOrg());
        patientReported.setUpdatedDate(new Date());

        {
            Transaction transaction = dataSession.beginTransaction();


            dataSession.saveOrUpdate(patientMaster);
            dataSession.saveOrUpdate(patientReported);
            transaction.commit();
        }

        return patientReported;
    }

    public VaccinationReported FHIR_EventVaccinationReported(OrgAccess orgAccess, Patient patient,PatientReported patientReported, Immunization immunization) throws Exception {
	VaccinationMaster vaccination = null;
	VaccinationReported vaccinationReported = null;

	Date administrationDate = null;
	{
	    Query query = dataSession.createQuery(
		    "from VaccinationReported where patientReported = ? and vaccinationReportedExternalLink = ?");
	    query.setParameter(0, patientReported);
	    query.setParameter(1, immunization.getId());
	    List<VaccinationReported> vaccinationReportedList = query.list();
	    if (vaccinationReportedList.size() > 0) {
		vaccinationReported = vaccinationReportedList.get(0);
		vaccination = vaccinationReported.getVaccination();
	    }
	} 
	if (vaccinationReported == null) {
	    vaccination = new VaccinationMaster();
	    vaccination.setPatient(patientReported.getPatient());
	    vaccinationReported = new VaccinationReported();
	    vaccinationReported.setVaccination(vaccination);
	    vaccinationReported.setPatientReported(patientReported);
	    vaccination.setVaccinationReported(vaccinationReported);
	    vaccination.setPatient(patientReported.getPatient());

	    ImmunizationHandler.vaccinationReportedFromFhirImmunization(vaccinationReported,immunization);
	}
	if (vaccinationReported.getUpdatedDate().before(patient.getBirthDate())) {
	    throw new Exception("Vaccination is reported as having been administered before the patient was born");
	}

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

	Transaction transaction = dataSession.beginTransaction();
	dataSession.saveOrUpdate(vaccination);
	dataSession.saveOrUpdate(vaccinationReported);
	vaccination.setVaccinationReported(vaccinationReported);
	dataSession.saveOrUpdate(vaccination);
	transaction.commit();
	return vaccinationReported;
    }
    
    public void FHIR_EventVaccinationDeleted(OrgAccess orgAccess, String id) {
	VaccinationReported vr = new VaccinationReported();
	VaccinationMaster vm = new VaccinationMaster();

        Query query = dataSession.createQuery(
                "from  VaccinationReported where vaccinationReportedExternalLink = ?");
        query.setParameter(0, id);
        List<VaccinationReported> vaccinationReportedList = query.list();
        if (vaccinationReportedList.size() > 0) {
            vr = vaccinationReportedList.get(0);
            vm =vr.getVaccination();
        }
        Transaction transaction = dataSession.beginTransaction();

        dataSession.delete(vr);
        dataSession.delete(vm);
        transaction.commit();
    }
    
}
