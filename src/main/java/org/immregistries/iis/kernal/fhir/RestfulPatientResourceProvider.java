package org.immregistries.iis.kernal.fhir;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
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
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * All resource providers must implement IResourceProvider
 */
public class RestfulPatientResourceProvider implements IResourceProvider {
    protected Session dataSession=null;
    protected OrgAccess orgAccess= null;
    protected OrgMaster orgMaster=null;
    private static SessionFactory factory;

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
    public Patient getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
        Patient patient =null;
        // Retrieve this patient in the database...
        Session dataSession = getDataSession();
        try {
            orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
            patient = getPatientById(theId.getIdPart(),dataSession,orgAccess );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }
        return patient;
    }



   @Create
    public MethodOutcome createPatient(RequestDetails theRequestDetails, @ResourceParam Patient thePatient) {
        PatientReported patientReported = null;
        List<Patient> matches= new ArrayList<Patient>();
       // System.err.println("l id du patient est " +thePatient.getId());
        if (thePatient.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        // Save this patient to the database...
       Session dataSession = getDataSession();
        //IdType idType = thePatient.getIdElement();
        //System.err.println(idType);

        //String id = idType.getIdPart();
       //System.err.println(id);

       try {
           orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
           FHIRHandler fhirHandler = new FHIRHandler(dataSession);
           matches= findMatch(dataSession,thePatient);
           patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,thePatient,null);

           for(Patient match : matches)
           {
               String encoded = Context.getCtx().newJsonParser().setPrettyPrint(true).encodeResourceToString(match);
               System.err.println(encoded);
           }
           System.err.println("found "+ matches.size() + " match(es)");
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
    public MethodOutcome updatePatient(RequestDetails theRequestDetails, @IdParam IdType theId , @ResourceParam Patient thePatient) {
        //TODO add validation method later
        PatientReported patientReported=null;
        //System.err.println(theId.getIdPart());

        Session dataSession = getDataSession();
        try {
            orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
            FHIRHandler fhirHandler = new FHIRHandler(dataSession);
            patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,thePatient,null);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }


        return new MethodOutcome();

    }


    @Delete()
    public MethodOutcome deletePatient(RequestDetails theRequestDetails, @IdParam IdType theId) {
        Session dataSession = getDataSession();
        try {
            orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
            deletePatientById(theId.getIdPart(),dataSession,orgAccess );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }


        return new MethodOutcome();
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

    public void deletePatientById(String id,Session dataSession,OrgAccess orgAccess) {
        PatientReported patientReported=null;
        PatientMaster patientMaster=null;

      {
          Query query = dataSession.createQuery(
                  "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
          query.setParameter(0, orgAccess.getOrg());
          query.setParameter(1, id);
          List<PatientReported> patientReportedList = query.list();
          if (patientReportedList.size() > 0) {
              patientReported = patientReportedList.get(0);
              patientMaster =patientReported.getPatient();
          }
      }
      {
          Transaction transaction = dataSession.beginTransaction();

          dataSession.delete(patientReported);
          dataSession.delete(patientMaster);

          //dataSession.delete(patientReported);
          transaction.commit();
      }


  }

  public ArrayList<Patient> findMatch(Session dataSession, Patient patient){
        ArrayList<Patient> matches = new ArrayList<Patient>();

              Query query = dataSession.createQuery(
                      "from PatientReported where patientNameLast = ? and patientNameFirst= ? ");
              query.setParameter(0, patient.getNameFirstRep().getFamily());
              query.setParameter(1,patient.getNameFirstRep().getGiven().get(0).toString());
              //query.setParameter(2, patient.getBirthDate());
              List<PatientReported> patientReportedList = query.list();


              if (patientReportedList.size() > 0)
              {

                  for(PatientReported patientReported : patientReportedList)
                  {
                      patient=PatientHandler.getPatient(null,null,patientReported);
                      matches.add(patient);
                  }

              }

        return matches;
  }
  /*@Read()
  public List<Patient> getMatches(RequestDetails theRequestDetails,  @ResourceParam Patient thePatient){
      Session dataSession = getDataSession();
      List<Patient> matches = new ArrayList<Patient>();
      try {

          orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
          matches = findMatch(dataSession,thePatient);

      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          dataSession.close();
      }


        return matches;

  }*/

}
