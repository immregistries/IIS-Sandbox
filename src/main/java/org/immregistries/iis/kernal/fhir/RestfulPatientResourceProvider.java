package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
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

import javax.servlet.http.HttpServletResponse;
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
        /*Patient patient= myPatients.get(theId.getIdPartAsLong());
        if(patient==null){
            throw new ResourceNotFoundException(theId);
        }*/
        return null;
    }

   @Create
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        PatientReported patientReported = null;

        if (thePatient.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        // Save this patient to the database...
       Session dataSession = getDataSession();
       try {
           if (orgAccess == null) {
               authenticateOrgAccess(PARAM_USERID,PARAM_PASSWORD,PARAM_FACILITYID,dataSession);
           }


           /*IParser jsonParser = Context.getCtx().newJsonParser();
           jsonParser.setPrettyPrint(true);
           String encoded = jsonParser.encodeResourceToString(thePatient);
           System.err.println(encoded);*/


           FHIRHandler fhirHandler = new FHIRHandler(dataSession);
           fhirHandler.processFIHR_Event(orgAccess,thePatient,null);



       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           dataSession.close();
       }
        return new MethodOutcome(new IdType(thePatient.getId()));

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
    public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam Patient thePatient) {
        //TODO add validation method later

        /*Long resourceId;
        try {
            resourceId = theId.getIdPartAsLong();

        } catch (DataFormatException e) {
            throw new InvalidRequestException("Invalid ID " + theId.getValue() + " - Must be numeric");
        }

        if (!myPatients.containsKey(resourceId)) {
            throw new ResourceNotFoundException(theId);
        }


        // ... perform the update ...
        myPatients.put(resourceId,thePatient);*/
        return new MethodOutcome();

    }


    @Delete()
    public MethodOutcome deletePatient(@IdParam IdType theId) {

        /*Long resourceId;
        try {
            resourceId = theId.getIdPartAsLong();

        } catch (DataFormatException e) {
            throw new InvalidRequestException("Invalid ID " + theId.getValue() + " - Must be numeric");
        }

        if (!myPatients.containsKey(resourceId)) {
            throw new ResourceNotFoundException(theId);
        }

        // .. Delete the patient ..
        //myPatients.remove(resourceId);*/

        return new MethodOutcome();
    }

    public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId,
                                           Session dataSession) {
        OrgMaster orgMaster = null;
        OrgAccess orgAccess = null;
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


}
