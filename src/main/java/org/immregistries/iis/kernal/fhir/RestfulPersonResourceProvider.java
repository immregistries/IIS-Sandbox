package org.immregistries.iis.kernal.fhir;


import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.logic.PersonHandler;

import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientLink;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;


public class RestfulPersonResourceProvider implements IResourceProvider {
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
  public Class<Person> getResourceType() {
    return Person.class;
  }

  @Read()
  public Person getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
    Person person = null;
    // Retrieve this person in the database...
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      person = getPersonById(theId.getIdPart(), dataSession, orgAccess);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return person;
  }

  /**
   * This methods asks to find and rebuild the person resource with the id provided
   * @param idPart The id of the person resource
   * @param dataSession The session
   * @param orgAccess the orgAccess
   * @return the Patient, null is no patient was found in the database
   */
  private Person getPersonById(String idPart, Session dataSession, OrgAccess orgAccess) {
    Person person = null;
    PatientReported patientReported = null;

    Query query = dataSession.createQuery(
          "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, idPart);
    @SuppressWarnings("unchecked")
	List<PatientReported> patientReportedList = query.list();

    if (!patientReportedList.isEmpty()) {
      patientReported = patientReportedList.get(0);
      person = PersonHandler.getPerson(patientReported);

      //add Link
      int linkId = patientReported.getPatientReportedId();
      Query queryLink = dataSession.createQuery("from PatientLink where patientMaster.id = ?");
      queryLink.setParameter(0, linkId);
      @SuppressWarnings("unchecked")
      List<PatientLink> patientLinkList = queryLink.list();

      if (!patientLinkList.isEmpty()) {
        for (PatientLink link : patientLinkList) {
          String ref = link.getPatientReported().getPatientReportedExternalLink();
          int assuranceLevel = link.getLevelConfidence();
          Person.PersonLinkComponent personLinkComponent = new Person.PersonLinkComponent();
          Reference reference = new Reference();
          reference.setReference("Patient/" + ref);
          if (assuranceLevel == 1) {
            person.addLink(personLinkComponent.setTarget(reference)
                .setAssurance(Person.IdentityAssuranceLevel.LEVEL2));
  
          } else {
            person.addLink(personLinkComponent.setTarget(reference)
                .setAssurance(Person.IdentityAssuranceLevel.LEVEL3));
          }
        }
      }
    }    
    return person;
  }

  /**
   * The "@Delete" annotation indicates that this method supports deleting an existing
   * resource (by ID)
   * @param theRequestDetails authentification access information
   *  @param theId This is the ID of the person to delete
   * @return This method returns a "MethodOutcome"
   */
  @Delete()
  public MethodOutcome deletePerson(RequestDetails theRequestDetails, @IdParam IdType theId) {
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      deletePersonById(theId.getIdPart(), dataSession, orgAccess);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome();
  }
  /**
   * This methods delete from the database the information about the person with the provided id
   * @param idPart The id of the resource to be deleted
   * @param dataSession The session
   * @param orgAccess The orgAccess
   */
  private void deletePersonById(String idPart, Session dataSession, OrgAccess orgAccess)
      throws Exception {
    PatientReported patientReported = null;
    PatientMaster patientMaster = null;

    {
      Query query = dataSession.createQuery(
          "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, idPart);
      @SuppressWarnings("unchecked")
	List<PatientReported> patientReportedList = query.list();
      if (!patientReportedList.isEmpty()) {

        patientReported = patientReportedList.get(0);
        patientMaster = patientReported.getPatient();
      }
      //Verify all links are already deleted if not ask to delete the patients first
    }

    {
      Query query = dataSession.createQuery(
          "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, idPart);
      @SuppressWarnings("unchecked")
	List<PatientReported> patientReportedList = query.list();
      if (!patientReportedList.isEmpty()) {
        patientReported = patientReportedList.get(0);
        patientMaster = patientReported.getPatient();
      }

    }
    {
      //Verify if all links are already deleted if not ask to delete
      // the several PatientReported and PatientLink first
      Query queryLink =
          dataSession.createQuery("from  PatientLink where patientMaster.patientId = ?");
      queryLink.setParameter(0, patientMaster.getPatientId());

      @SuppressWarnings("unchecked")
	List<PatientLink> patientLinkList = queryLink.list();

      if (!patientLinkList.isEmpty()) {
        throw new Exception("The patients linked  to Person/" + idPart + " must be deleted first");
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
