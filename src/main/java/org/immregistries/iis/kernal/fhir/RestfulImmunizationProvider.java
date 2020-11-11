package org.immregistries.iis.kernal.fhir;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.immregistries.iis.kernal.logic.*;
import org.hl7.fhir.r4.model.Patient;
import org.immregistries.iis.kernal.logic.ImmunizationHandler;
import org.immregistries.iis.kernal.logic.PatientHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.repository.PatientRepository;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class RestfuImmunizationProvider implements IResourceProvider {
    protected Session dataSession = null;
    public static final String PARAM_USERID = "TELECOM NANCY";
    public static final String PARAM_PASSWORD = "1234";
    public static final String PARAM_FACILITYID = "TELECOMNANCY";
    protected OrgAccess orgAccess = null;
    protected OrgMaster orgMaster = null;
    protected PatientReported patientReported = null;
    protected VaccinationReported vaccinationReported = null;
    private static SessionFactory factory;

    @Override
    public Class<Immunization> getResourceType() {
	return Immunization.class;
    }

    public static Session getDataSession() {
	if (factory == null) {
	    factory = new AnnotationConfiguration().configure().buildSessionFactory();
	}
	return factory.openSession();
    }

    @Create
    public MethodOutcome createImmunization(@ResourceParam Immunization theImmunization) {
        vaccinationReported = null;
        //System.err.println("l id du patient est " +theImmunization.getId());
        if (theImmunization.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        Session dataSession = getDataSession();
        try {
            if (orgAccess == null) {
                authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
            }
            FHIRHandler fhirHandler = new FHIRHandler(dataSession);
            PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession, theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
            Patient patient = PatientHandler.patientReportedToFhirPatient(pr);
            
            fhirHandler.processFIHR_Event(orgAccess,patient,theImmunization);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }
        return new MethodOutcome(new IdType(theImmunization.getIdentifier().get(0).getValue()));
    }

    @Read()
    public Immunization getResourceById(@IdParam IdType theId) {
	Immunization immunization = null;
	Session dataSession = getDataSession();
	try {
	    if (orgAccess == null) {
		authenticateOrgAccess(PARAM_USERID, PARAM_PASSWORD, PARAM_FACILITYID, dataSession);
	    }
	    immunization = getImmunizationById(theId.getIdPart(), dataSession, orgAccess);
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    dataSession.close();
	}
	return immunization;
    }
    
    @Update()
    public MethodOutcome updateImmunization(@IdParam IdType theId , @ResourceParam Immunization theImmunization) {
        vaccinationReported = null;
        if (theImmunization.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        Session dataSession = getDataSession();
        try {
            if (orgAccess == null) {
                authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
            }
            FHIRHandler fhirHandler = new FHIRHandler(dataSession);
            PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession, theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
            Patient patient = PatientHandler.patientReportedToFhirPatient(pr);
            
            fhirHandler.processFIHR_Event(orgAccess,patient,theImmunization);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }
        return new MethodOutcome(new IdType(theImmunization.getIdentifier().get(0).getValue()));
    }
    
    @Delete()
    public MethodOutcome deleteImmunization(@IdParam IdType theId) {
        Session dataSession = getDataSession();
        try {
            if (orgAccess == null) {
                authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
            }
            FHIRHandler fhirHandler = new FHIRHandler(dataSession);

            fhirHandler.FHIR_EventVaccinationDeleted(orgAccess,theId.getIdPart());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }


        return new MethodOutcome();
    }


    public static Immunization getImmunizationById(String id, Session dataSession, OrgAccess orgAccess) {
	Immunization immunization = null;
	VaccinationReported vaccinationReported = null;
	PatientReported patientReported = null;
	OrgLocation orgLocation = null;
	{
	    Query query = dataSession
		    .createQuery("from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
	    query.setParameter(0, orgAccess.getOrg());
	    query.setParameter(1, id);
	    List<PatientReported> patientReportedList = query.list();
	    if (patientReportedList.size() > 0) {
		patientReported = patientReportedList.get(0);
	    }
	}
	{
	    Query query = dataSession.createQuery(
		    "from VaccinationReported where vaccinationReportedExternalLink = ?");
	    query.setParameter(0, id);
	    List<VaccinationReported> vaccinationReportedList = query.list();
	    if (vaccinationReportedList.size() > 0) {
		vaccinationReported = vaccinationReportedList.get(0);
		immunization = ImmunizationHandler.getImmunization(vaccinationReported.getOrgLocation(),
			vaccinationReported, patientReported);
	    }
	}
	/*
	 * { Query query = dataSession.createQuery(
	 * "from OrgLocation where  org_facility_code = ?"); query.setParameter(0,
	 * vaccinationReported.getOrgLocation()); List<PatientReported>
	 * patientReportedList = query.list(); if (patientReportedList.size() > 0) {
	 * patientReported = patientReportedList.get(0); } }
	 */
	return immunization;
    }

    public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId, Session dataSession) {
	{
	    Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
	    query.setParameter(0, facilityId);
	    List<OrgMaster> orgMasterList = query.list();
	    if (orgMasterList.size() > 0) {
		orgMaster = orgMasterList.get(0);
	    } else {
		orgMaster = new OrgMaster();
		orgMaster.setOrganizationName(facilityId);
		orgAccess = new OrgAccess();
		orgAccess.setOrg(orgMaster);
		orgAccess.setAccessName(userId);
		orgAccess.setAccessKey(password);
		Transaction transaction = dataSession.beginTransaction();
		dataSession.save(orgMaster);
		dataSession.save(orgAccess);
		transaction.commit();
	    }

	}
	if (orgAccess == null) {
	    Query query = dataSession.createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
	    query.setParameter(0, userId);
	    query.setParameter(1, password);
	    query.setParameter(2, orgMaster);
	    List<OrgAccess> orgAccessList = query.list();
	    if (orgAccessList.size() != 0) {
		orgAccess = orgAccessList.get(0);
	    }
	}
	return orgAccess;
    }

}
