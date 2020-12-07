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
           //matches= findMatch(dataSession,thePatient);
           patientReported = fhirHandler.FIHR_EventPatientReported(orgAccess,thePatient,null);

           /*for(Patient match : matches)
           {
               String encoded = Context.getCtx().newJsonParser().setPrettyPrint(true).encodeResourceToString(match);
               System.err.println(encoded);
           }
           System.err.println("found "+ matches.size() + " match(es)");*/
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
        int idInt = 0;
        boolean isExternalLink = false;
        try {
            idInt = Integer.parseInt(id);
        } catch(NumberFormatException | NullPointerException e) {
            isExternalLink = true;
        }
        //System.err.println("the id is " + id);
        {
            Query query;
            if (isExternalLink){
                query = dataSession.createQuery(
                        "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
                query.setParameter(0, orgAccess.getOrg());
                query.setParameter(1, id);
            }else {
                query = dataSession.createQuery(
                        "from PatientReported where orgReported = ? and patientReportedId = ?");
                query.setParameter(0, orgAccess.getOrg());
                query.setParameter(1, idInt);
            }

            List<PatientReported> patientReportedList = query.list();
            if (patientReportedList.size() > 0) {
                patientReported = patientReportedList.get(0);
                patient =PatientHandler.getPatient(null,null,patientReported);
                //TODO add patientLink
                System.err.println(patientReported.getPatientReportedId());
                int linkId = patientReported.getPatientReportedId();
               Query queryLink = dataSession.createQuery(
                        "from PatientLink where patientReported.id = ?");
                queryLink.setParameter(0, linkId);
               List<PatientLink> patientLinkList = queryLink.list();
                System.err.println(queryLink.list().size());

                if(patientLinkList.size()>0){
                    //System.err.println(queryLink.list().size());

                    for(PatientLink link : patientLinkList){
                        //System.err.println(link.getPatientMaster());
                        Query queryMaster = dataSession.createQuery(
                                "from PatientMaster where patientId = ?");
                        queryMaster.setParameter(0, link.getPatientMaster().getPatientId());
                        List<PatientMaster> patientMasterList = queryMaster.list();
                        if(patientMasterList.size()>0){
                            System.err.println(patientMasterList.get(0).getPatientExternalLink());
                            String ref = patientMasterList.get(0).getPatientExternalLink();
                            Patient.PatientLinkComponent patientLinkComponent= new Patient.PatientLinkComponent();
                            Reference reference = new Reference();
                            //reference.setReference("http://localhost:8080/iis-sandbox/fhir/Org1/Patient/"+ref);
                            //reference.setReference(ref);
                            reference.setReference("Patient/"+ref);


                            patient.addLink(patientLinkComponent.setOther(reference));

                        }



                    }


               }





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
          transaction.commit();
      }
  }

  /*public ArrayList<Patient> findMatch(Session dataSession, Patient patient){
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
  }*/
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
