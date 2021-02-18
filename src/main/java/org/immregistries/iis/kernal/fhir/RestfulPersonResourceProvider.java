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
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;


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
    Session dataSession = getDataSession();
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

    Query query =
        dataSession.createQuery("from PatientReported where patientReportedExternalLink = ?");
    query.setParameter(0, idPart);
    @SuppressWarnings("unchecked")
    List<PatientReported> patientReportedList = query.list();

    if (patientReportedList.size() > 0) {
      patientReported = patientReportedList.get(0);
      person = PersonHandler.getPerson(patientReported);
    }
    //add Link
    System.err.println("adding links");

    int linkId = patientReported.getPatient().getPatientId();
    Query queryLink = dataSession.createQuery("from PatientLink where patientMaster.id = ?");
    queryLink.setParameter(0, linkId);
    System.err.println("partienMasterId " + linkId);

    List<PatientLink> patientLinkList = queryLink.list();

    if (patientLinkList.size() > 0) {
      System.err.println("found links ");
      for (PatientLink link : patientLinkList) {
        String ref = link.getPatientReported().getPatientReportedExternalLink();
        int assuranceLevel = link.getLevelConfidence();
        Person.PersonLinkComponent personLinkComponent = new Person.PersonLinkComponent();
        Reference reference = new Reference();
        reference.setReference("Patient/" + ref);
        if (assuranceLevel == 1) {
          person.addLink(personLinkComponent.setTarget(reference)
              .setAssurance(Person.IdentityAssuranceLevel.LEVEL2));
          System.err.println("adding links level 1");

        } else {
          person.addLink(personLinkComponent.setTarget(reference)
              .setAssurance(Person.IdentityAssuranceLevel.LEVEL3));
          System.err.println("adding links level 2");
        }
      }

    }
    return person;
  }


  @Create
  public MethodOutcome createPatient(RequestDetails theRequestDetails,
      @ResourceParam Person thePerson) {

    return new MethodOutcome();

  }


  @Update
  public MethodOutcome updatePerson(RequestDetails theRequestDetails, @IdParam IdType theId,
      @ResourceParam Person thePerson) {
    return new MethodOutcome();
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
    Session dataSession = getDataSession();
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
  private void deletePersonById(String idPart, Session dataSession, OrgAccess orgAccess) throws Exception {
      PatientMaster patientMaster = null;

      {
        Query query = dataSession.createQuery(
            "from  PatientMaster where patientExternalLink = ?");

        query.setParameter(0, idPart);
        @SuppressWarnings("unchecked")
        List<PatientMaster> patientMasterList = query.list();
        if (patientMasterList.size() > 0) {

          patientMaster = patientMasterList.get(0);
        }
      }

      {
        //Verify if all links and patient reported are already deleted if not ask to delete
        // the several PatientReported and PatientLink first

          Query query = dataSession.createQuery(
              "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
          query.setParameter(0, orgAccess.getOrg());
          query.setParameter(1, idPart);
          @SuppressWarnings("unchecked")
          List<PatientReported> patientReportedList = query.list();

          System.err.println(patientReportedList.size());
          if (patientReportedList.size() > 0) {

            throw new Exception(
                "The patient  Patient/" + idPart + " must be deleted first");
          }
        }


        Query queryLink =
            dataSession.createQuery("from  PatientLink where patientMaster.patientId = ?");
        queryLink.setParameter(0, patientMaster.getPatientId());

        @SuppressWarnings("unchecked")
        List<PatientLink> patientLinkList = queryLink.list();

        if (patientLinkList.size() > 0) {

          throw new Exception(
              "The patients linked  to Person/" + idPart + " must be deleted first");
        }


    {
      Transaction transaction = dataSession.beginTransaction();

      dataSession.delete(patientMaster);
      transaction.commit();
    }
  }


}
