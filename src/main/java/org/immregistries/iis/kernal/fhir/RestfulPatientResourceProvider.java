package org.immregistries.iis.kernal.fhir;


import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.logic.PatientHandler;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientLink;

import org.immregistries.iis.kernal.model.PatientReported;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;


/**
 * All resource providers must implement IResourceProvider
 */
public class RestfulPatientResourceProvider implements IResourceProvider {
  protected Session dataSession = null;
  protected OrgAccess orgAccess = null;
  protected OrgMaster orgMaster = null;
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
   *@param theRequestDetails authentification access information
   *@param theId the id of the resource to be read
   *    The read operation takes one parameter, which must be of type
   *    IdType and must be annotated with the "@Read.IdParam" annotation.
   * @return
   *    Returns a resource matching this identifier, or null if none exists.
   */
  @Read()
  public Patient getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
    Patient patient = null;
    // Retrieve this patient in the database...
    dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      patient = getPatientById(theId.getIdPart(), dataSession, orgAccess);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return patient;
  }


  /**
   *The "@Create" annotation indicates that this method supports the
   * create operation.
   * @param theRequestDetails authentification access information
   * @param thePatient patient resource body
   * @return This method returns a MethodOutcome object which contains
   * the ID of the new patient
   */
  @Create
  public MethodOutcome createPatient(RequestDetails theRequestDetails,
      @ResourceParam Patient thePatient) {
    if (thePatient.getIdentifierFirstRep().isEmpty()) {
      throw new UnprocessableEntityException("No identifier supplied");
    }
    // Save this patient to the database...
    dataSession = getDataSession();
    PatientReported pr = new PatientReported();
    orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
    FHIRHandler fhirHandler = new FHIRHandler(dataSession);

    pr = fhirHandler.fhirEventPatientReported(orgAccess, thePatient, null);

    MethodOutcome retVal = new MethodOutcome();
    retVal.setId(new IdType("Patient", pr.getPatientReportedExternalLink(), "1"));

    dataSession.close();
    return retVal;

  }

  /**
   * The "@Update" annotation indicates that this method supports replacing an existing
   * resource (by ID) with a new instance of that resource.
   *
   * @param theId      This is the ID of the patient to update
   * @param thePatient This is the actual resource to update
   * @return This method returns a "MethodOutcome"
   */

  @Update
  public MethodOutcome updatePatient(RequestDetails theRequestDetails, @IdParam IdType theId,
      @ResourceParam Patient thePatient) {
    dataSession = getDataSession();
    PatientReported pr = new PatientReported();
    MethodOutcome retVal = new MethodOutcome();
    try {
      // FHIR Specification, need url id to be same as resource id

      // if (!thePatient.getId().equals(theId.getIdPart().split("Patient/")[0])) {
      //   throw new InvalidRequestException("Resource Id " + theId.getIdPart().split("Patient/")[0] + " different from Request Id " + theId.getIdPart() );
      // } 

      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);

      pr = fhirHandler.fhirEventPatientReported(orgAccess, thePatient, null);
      retVal.setResource(getPatientById(pr.getPatientReportedExternalLink(), dataSession, orgAccess));
      retVal.setId(new IdType("Patient", pr.getPatientReportedExternalLink()));
      dataSession.close();
    } catch (Exception e) {
      e.printStackTrace();
      dataSession.close();
      throw e;
    }
    
    return retVal ;
  }

  /**
   * The "@Delete" annotation indicates that this method supports deleting an existing
   * resource (by ID)
   * @param theRequestDetails authentification access information
   *  @param theId This is the ID of the patient to delete
   * @return This method returns a "MethodOutcome"
   */
  @Delete()
  public MethodOutcome deletePatient(RequestDetails theRequestDetails, @IdParam IdType theId) {
    dataSession = getDataSession();
    orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
    deletePatientById(theId.getIdPart(), dataSession, orgAccess);
    dataSession.close();
    return new MethodOutcome();
  }

  /**
   * This methods asks to find and rebuild the patient resource with the id provided
   * @param id The id of the patient resource
   * @param dataSession The session
   * @param orgAccess the orgAccess
   * @return the Patient, null is no patient was found in the database
   */
  public static Patient getPatientById(String id, Session dataSession, OrgAccess orgAccess) {
    Patient patient = null;
    PatientReported patientReported = null;
    
    {
      Query query = dataSession.createQuery(
          "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, id);

      @SuppressWarnings("unchecked")
	    List<PatientReported> patientReportedList = query.list();
      if (!patientReportedList.isEmpty()) {
        patientReported = patientReportedList.get(0);
        patient = PatientHandler.getFhirPatient(null, null, patientReported);

        int linkId = patientReported.getPatientReportedId();
        Query queryLink = dataSession.createQuery("from PatientLink where patientReported.id = ?");
        queryLink.setParameter(0, linkId);
        @SuppressWarnings("unchecked")
		    List<PatientLink> patientLinkList = queryLink.list();

        if (!patientLinkList.isEmpty()) {
          for (PatientLink link : patientLinkList) {
            String ref = link.getPatientMaster().getPatientExternalLink();
            Patient.PatientLinkComponent patientLinkComponent = new Patient.PatientLinkComponent();
            Reference reference = new Reference();
            reference.setReference("Person/" + ref);
            patient.addLink(patientLinkComponent.setOther(reference));

          }
        }
      }
    }
    return patient;
  }

  /**
   * This methods delete from the database the information about the patient with the provided id
   * @param id The id of the resource to be deleted
   * @param dataSession The session
   * @param orgAccess The orgAccess
   */
  //We can delete only Patient with no link
  public void deletePatientById(String id, Session dataSession, OrgAccess orgAccess) {
    PatientReported patientReported = null;
    PatientLink patientLink = null;

    {
      Query query = dataSession.createQuery(
          "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, id);
      @SuppressWarnings("unchecked")
	    List<PatientReported> patientReportedList = query.list();
      if (!patientReportedList.isEmpty()) {
        patientReported = patientReportedList.get(0);
        // Deleting possible links
        Query queryLink = dataSession.createQuery(
            "from  PatientLink where patientReported.patientReportedId=?");
        queryLink.setParameter(0, patientReported.getPatientReportedId());
        @SuppressWarnings("unchecked")
        List<PatientLink> patientLinkList = queryLink.list();
        if (!patientLinkList.isEmpty()) {
          patientLink = patientLinkList.get(0);
        }
      }
    }
    {
      Transaction transaction = dataSession.beginTransaction();
      dataSession.delete(patientReported);
      if (patientLink != null) {
        dataSession.delete(patientLink);
      }
      //dataSession.delete(patientMaster);
      transaction.commit();
    }
  }



}
