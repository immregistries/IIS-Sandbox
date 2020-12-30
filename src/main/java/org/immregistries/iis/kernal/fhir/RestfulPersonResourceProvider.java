package org.immregistries.iis.kernal.fhir;

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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.logic.FHIRHandler;
import org.immregistries.iis.kernal.logic.PersonHandler;
import org.immregistries.iis.kernal.model.*;

import java.util.ArrayList;
import java.util.List;

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

  private Person getPersonById(String idPart, Session dataSession, OrgAccess orgAccess) {
    Person person = null;
    PatientReported patientReported = null;

    Query query =
        dataSession.createQuery("from PatientReported where patientReportedExternalLink = ?");
    query.setParameter(0, idPart);
    List<PatientReported> patientReportedList = query.list();

    if (patientReportedList.size() > 0) {
      patientReported = patientReportedList.get(0);
      person = PersonHandler.getPerson(patientReported);
    }
    //add Link

    int linkId = patientReported.getPatientReportedId();
    Query queryLink = dataSession.createQuery("from PatientLink where patientMaster.id = ?");
    queryLink.setParameter(0, linkId);
    List<PatientLink> patientLinkList = queryLink.list();

    if (patientLinkList.size() > 0) {
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

  private void deletePersonById(String idPart, Session dataSession, OrgAccess orgAccess)
      throws Exception {
    PatientReported patientReported = null;
    PatientMaster patientMaster = null;

    {
      Query query = dataSession.createQuery(
          "from  PatientReported where orgReported = ? and patientReportedExternalLink = ?");
      query.setParameter(0, orgAccess.getOrg());
      query.setParameter(1, idPart);
      List<PatientReported> patientReportedList = query.list();
      System.err.println(orgAccess.getOrg());
      System.err.println(idPart);
      System.err.println(patientReportedList.size());
      if (patientReportedList.size() > 0) {

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
      List<PatientReported> patientReportedList = query.list();
      System.err.println(orgAccess.getOrg());
      System.err.println(idPart);
      System.err.println(patientReportedList.size());
      if (patientReportedList.size() > 0) {

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

      List<PatientLink> patientLinkList = queryLink.list();

      if (patientLinkList.size() > 0) {

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
