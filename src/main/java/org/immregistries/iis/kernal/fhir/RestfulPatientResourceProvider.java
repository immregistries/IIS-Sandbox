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
import org.immregistries.iis.kernal.model.*;
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
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

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
        if (thePatient.getIdentifierFirstRep().isEmpty()) {
            throw new UnprocessableEntityException("No identifier supplied");
        }
        // Save this patient to the database...
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
        PatientReported patientReported=null;
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

                int linkId = patientReported.getPatientReportedId();
               Query queryLink = dataSession.createQuery(
                        "from PatientLink where patientReported.id = ?");
                queryLink.setParameter(0, linkId);
               List<PatientLink> patientLinkList = queryLink.list();

                if(patientLinkList.size()>0){
                    for(PatientLink link : patientLinkList){
                            String ref = link.getPatientMaster().getPatientExternalLink();
                            Patient.PatientLinkComponent patientLinkComponent= new Patient.PatientLinkComponent();
                            Reference reference = new Reference();
                            reference.setReference("Person/"+ref);
                            patient.addLink(patientLinkComponent.setOther(reference));

                    }
               }
            }
        }
        return patient;
    }
    //We can delete only Patient with no link
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
              System.err.println("Lid du patient Master est  " +patientMaster.getPatientExternalLink());



          }
      }
      {
          Transaction transaction = dataSession.beginTransaction();
          dataSession.delete(patientReported);

          dataSession.delete(patientMaster);


          transaction.commit();
      }
  }



}
