package org.immregistries.iis.kernal.fhir;


import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.logic.*;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import java.util.*;

/**
 * All resource providers must implement IResourceProvider
 */
public class RestfulPatientResourceProvider implements IResourceProvider {
    protected Session dataSession=null;
    public static final String PARAM_USERID = "TELECOM NANCY";
    public static final String PARAM_PASSWORD = "1234";
    public static final String PARAM_FACILITYID = "TELECOMNANCY";
    protected OrgAccess orgAccess= null;
    protected OrgMaster orgMaster=null;
    protected PatientReported patientReported=null;
    private static SessionFactory factory;

    public RestfulPatientResourceProvider() {
    }

    public static Session getDataSession() {
        if (factory == null) {
            factory = new AnnotationConfiguration().configure().buildSessionFactory();
        }
        return factory.openSession();
    }

    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }


    /**
     * The "@Read" annotation indicates that this method supports the
     * read operation. Read operations should return a single resource
     * instance.
     *
     * @param theId
     *    The read operation takes one parameter, which must be of type
     *    IdType and must be annotated with the "@Read.IdParam" annotation.
     * @return
     *    Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public Patient getResourceById(@IdParam IdType theId) {
        Patient patient =null;
        // Retrieve this patient in the database...
        Session dataSession = getDataSession();
        try {
            if (orgAccess == null) {
                authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
            }
            patient = getPatientById(theId.getIdPart(),dataSession,orgAccess );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }
        return patient;
    }

   @Create
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        patientReported = null;
        System.err.println("l id du patient est " +thePatient.getId());
        if (thePatient.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        // Save this patient to the database...
       Session dataSession = getDataSession();
       try {
           if (orgAccess == null) {
               authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
           }
           FHIRHandler fhirHandler = new FHIRHandler(dataSession);
           patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,thePatient,null);
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           dataSession.close();
       }
        return new MethodOutcome(new IdType(thePatient.getIdentifier().get(0).getValue()));

    }

    /**
     * The "@Update" annotation indicates that this method supports replacing an existing
     * resource (by ID) with a new instance of that resource.
     *
     * @param theId      This is the ID of the patient to update
     * @param thePatient This is the actual resource to save
     * @return This method returns a "MethodOutcome"
     */

    @Update
    public MethodOutcome updatePatient(@IdParam IdType theId , @ResourceParam Patient thePatient) {
        //TODO add validation method later

        System.err.println(theId.getIdPart());

        Session dataSession = getDataSession();
        try {
            if (orgAccess == null) {
                authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
            }


            FHIRHandler fhirHandler = new FHIRHandler(dataSession);
            patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,thePatient,null);
            //fhirHandler.processFIHR_Event(orgAccess,thePatient,null);



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }


        return new MethodOutcome();

    }


    @Delete()
    public MethodOutcome deletePatient(@IdParam IdType theId) {


        return new MethodOutcome();
    }

    public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId,
                                           Session dataSession) {

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
            Query query = dataSession
                    .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
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

    public static Patient getPatientById(String id, Session dataSession,OrgAccess orgAccess ){
        Patient patient = null;
        PatientReported patientReported =null;
        {
            Query query = dataSession.createQuery(
                    "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
            query.setParameter(0, orgAccess.getOrg());
            query.setParameter(1, id);
            List<PatientReported> patientReportedList = query.list();
            if (patientReportedList.size() > 0) {
                patientReported = patientReportedList.get(0);
                patient =PatientHandler.getPatient(null,null,patientReported);


            }
        }
        return patient;
    }


}
