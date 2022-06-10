package org.immregistries.iis.kernal.fhir;

import java.util.List;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Immunization;
import org.immregistries.iis.kernal.logic.*;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.logic.ImmunizationHandler;
import org.immregistries.iis.kernal.logic.PatientHandler;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.repository.PatientRepository;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class RestfulImmunizationProvider implements IResourceProvider {
  protected Session dataSession = null;
  protected OrgAccess orgAccess = null;
  protected OrgMaster orgMaster = null;
  protected PatientReported patientReported = null;
  protected VaccinationReported vaccinationReported = null;
  private static SessionFactory factory;

  /**
   * The getResourceType method comes from IResourceProvider, and must
   * be overridden to indicate what type of resource this provider
   * supplies.
   */
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

  /**
   *The "@Create" annotation indicates that this method supports the
   * create operation.
   * @param theRequestDetails authentification access information
   * @param theImmunization immunization resource body
   * @return This method returns a MethodOutcome object which contains
   * the ID of the new immunization
   */
  @Create
  public MethodOutcome createImmunization(RequestDetails theRequestDetails,
      @ResourceParam Immunization theImmunization) {
    vaccinationReported = null;
    if (theImmunization.getIdentifierFirstRep().isEmpty()) {
      throw new UnprocessableEntityException("No identifier supplied");
    }
    dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);
      PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession,
          theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
      if (null != pr) {
        Patient patient = PatientHandler.patientReportedToFhir(pr);
        fhirHandler.processFhirEvent(orgAccess, patient, theImmunization);
      } else {
        throw new InvalidRequestException("No patient Found with the identifier supplied");
      }
    } catch (InvalidRequestException e) {
      e.printStackTrace();
      throw e;
    } finally {
      dataSession.close();
    }
    return new MethodOutcome(new IdType("Immunization",theImmunization.getIdentifier().get(0).getValue()));
  }
  /**
   * This methods asks to find and rebuild the person resource with the id provided
   * @param theRequestDetails authentification access information
   * @param theId The id of the immunization resource
   *
   *
   * @return the Immunization, null is none was found in the database
   */
  @Read()
  public Immunization getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
    Immunization immunization = null;
    dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);

      immunization =
          getImmunizationById(theRequestDetails, theId.getIdPart(), dataSession, orgAccess);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return immunization;
  }

  /**
   * The "@Update" annotation indicates that this method supports replacing an existing
   * resource (by ID) with a new instance of that resource.
   *
   * @param theId      This is the ID of the Immunization to update
   * @param theImmunization This is the actual resource to update
   * @return This method returns a "MethodOutcome"
   */
  @Update()
  public MethodOutcome updateImmunization(RequestDetails theRequestDetails, @IdParam IdType theId,
      @ResourceParam Immunization theImmunization) {
    vaccinationReported = null;
    if (theImmunization.getIdentifierFirstRep().isEmpty()) {
      throw new UnprocessableEntityException("No identifier supplied");
    }
    dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      FHIRHandler fhirHandler = new FHIRHandler(dataSession);
      PatientReported pr = PatientRepository.getPatientFromExternalId(orgAccess, dataSession,
          theImmunization.getPatient().getReference().substring(8)); // the id patient starts with Patient/ so we cut it
      Patient patient = PatientHandler.patientReportedToFhir(pr);

      fhirHandler.processFhirEvent(orgAccess, patient, theImmunization);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome(new IdType("Immunization",theImmunization.getIdentifier().get(0).getValue()));
  }
  /**
   * The "@Delete" annotation indicates that this method supports deleting an existing
   * resource (by ID)
   * @param theRequestDetails authentification access information
   *  @param theId This is the ID of the immunization to delete
   * @return This method returns a "MethodOutcome"
   */
  @Delete()
  public MethodOutcome deleteImmunization(RequestDetails theRequestDetails, @IdParam IdType theId) {
    dataSession = getDataSession();
    try {
      orgAccess = Authentication.authenticateOrgAccess(theRequestDetails, dataSession);
      fhirEventVaccinationDeleted(orgAccess, theId.getIdPart());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      dataSession.close();
    }
    return new MethodOutcome();
  }

  /**
   * This methods asks to find and rebuild the immunization resource with the id provided
   * @param id The id of the patient resource
   * @param dataSession The session
   * @param orgAccess the orgAccess
   * @return the Immunization, null is none was found in the database
   */
  public static Immunization getImmunizationById(RequestDetails theRequestDetails, String id,
      Session dataSession, OrgAccess orgAccess) {
    Immunization immunization = null;
    VaccinationReported vaccinationReported = null;

    {
      Query query = dataSession
          .createQuery("from VaccinationReported where vaccinationReportedExternalLink = ?");
      query.setParameter(0, id);
      @SuppressWarnings("unchecked")
      List<VaccinationReported> vaccinationReportedList = query.list();
      if (!vaccinationReportedList.isEmpty()) {
        vaccinationReported = vaccinationReportedList.get(0);
        if (vaccinationReported.getPatientReported().getOrgReported().getOrgId() != orgAccess.getOrgAccessId()){
          immunization = ImmunizationHandler.getImmunization(theRequestDetails, vaccinationReported);
        }
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

  /**
   * This method deletes the vacinationReported from the database with the provided id
   * @param orgAccess the orgAcess of the organization
   * @param id the id of the vaccinationReported to be deleted
   */
  public void fhirEventVaccinationDeleted(OrgAccess orgAccess, String id) {
    VaccinationReported vr = new VaccinationReported();
    VaccinationMaster vm = new VaccinationMaster();

    Query query = dataSession.createQuery(
        "from  VaccinationReported where vaccinationReportedExternalLink = ?");
    query.setParameter(0, id);
    @SuppressWarnings("unchecked")
    List<VaccinationReported> vaccinationReportedList = query.list();
    if (!vaccinationReportedList.isEmpty()) {
      vr = vaccinationReportedList.get(0);
      vm = vr.getVaccination();
    }
    Transaction transaction = dataSession.beginTransaction();

    dataSession.delete(vr);
    dataSession.delete(vm);
    transaction.commit();
  }
}