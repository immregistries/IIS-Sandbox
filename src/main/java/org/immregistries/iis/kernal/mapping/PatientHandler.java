package org.immregistries.iis.kernal.mapping;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

import java.util.Date;
import java.util.List;

public class PatientHandler {

  private PatientHandler(){}

	public static PatientReported patientReportedFromFhir(Patient p) {
	  PatientReported patientReported = new PatientReported();
	  patientReported.setPatientReportedId(p.getId());
	  fillPatientReportedFromFhir(patientReported,p);
	  PersonHandler.getPatientMasterFromFhir(patientReported.getPatient(), p);
	  return patientReported;
	}

  /**
   * This method set the patientReported information based on the patient information
   * @param patientReported the patientReported
   * @param p the Patient resource
   */
  public static void fillPatientReportedFromFhir(PatientReported patientReported, Patient p) {
     patientReported.setPatientReportedId(p.getIdentifierFirstRep().getValue()); // TODO set patient Master
    patientReported.setReportedDate(new Date());
    patientReported.setPatientReportedExternalLink(p.getIdentifier().get(0).getValue());

    // Name
    HumanName name = p.getNameFirstRep();
    patientReported.setPatientNameLast(name.getFamily());
    if (name.getGiven().size() > 0) {
      patientReported.setPatientNameFirst(name.getGiven().get(0).getValueNotNull());
    }
    if (name.getGiven().size() > 1) {
      patientReported.setPatientNameMiddle(name.getGiven().get(1).getValueNotNull());
    }

    patientReported.setPatientBirthDate(p.getBirthDate());
    patientReported.setPatientSex(String.valueOf(p.getGender().toString().charAt(0)));

	  switch (p.getGender()) {
		  case MALE:
			  patientReported.setPatientSex("M");
			  break;
		  case FEMALE:
			  patientReported.setPatientSex("F");
			  break;
		  case OTHER:
		  default:
			  patientReported.setPatientSex("");
			  break;
	  }

    // Address
    Address address = p.getAddressFirstRep();
    if (address.getLine().size() > 0) {
      patientReported.setPatientAddressLine1(address.getLine().get(0).getValueNotNull());
    }
    if (address.getLine().size() > 1) {
      patientReported.setPatientAddressLine2(address.getLine().get(1).getValueNotNull());
    }
    patientReported.setPatientAddressCity(address.getCity());
    patientReported.setPatientAddressState(address.getState());
    patientReported.setPatientAddressZip(address.getPostalCode());
    patientReported.setPatientAddressCountry(address.getCountry());
    patientReported.setPatientAddressCountyParish(address.getDistrict());

    for (ContactPoint telecom : p.getTelecom()) {
      if (null != telecom.getSystem()) {
        if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
          patientReported.setPatientPhone(telecom.getValue());
        } else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
          patientReported.setPatientEmail(telecom.getValue());
        }
      }
    }

//     patientReported.setPatientBirthFlag(p.getBirthDate().toString());
    if (null != p.getMultipleBirth()) {
		patientReported.setPatientBirthFlag("Y");
      patientReported.setPatientBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
    } else  {
		 patientReported.setPatientBirthFlag("N");
	 }

    if (null != p.getDeceased()) {
      if (p.getDeceasedBooleanType().isBooleanPrimitive()) {
        patientReported
            .setPatientDeathFlag(String.valueOf(p.getDeceasedBooleanType().toString().charAt(0))); // Y
        // or
        // N
      }
      if (p.getDeceased().isDateTime()) {
        patientReported.setPatientDeathDate(p.getDeceasedDateTimeType().getValue());

      }
    }

    // patientReported.setRegistryStatusIndicator(p.getActive());
    // Patient Contact / Guardian
    Patient.ContactComponent contact = p.getContactFirstRep();
    patientReported.setGuardianLast(contact.getName().getFamily());
    if (p.getContactFirstRep().getName().getGiven().size() > 0) {
      patientReported.setGuardianFirst(contact.getName().getGiven().get(0).getValueNotNull());
    }
    if (p.getContactFirstRep().getName().getGiven().size() > 1) {
      patientReported.setGuardianMiddle(contact.getName().getGiven().get(1).getValueNotNull());
    }
    patientReported.setGuardianRelationship(contact.getRelationshipFirstRep().getText());

    // PatientMaster Ressource

    PatientMaster patientMaster = patientReported.getPatient();
	 if (patientMaster == null) {
		 patientMaster = new PatientMaster();
	 }
    if (patientMaster.getPatientNameLast().equals("")) { //TODO improve this condition

      patientMaster.setPatientId(patientReported.getPatientReportedId());
      patientMaster.setPatientExternalLink(patientReported.getPatientReportedExternalLink());
      patientMaster.setPatientNameLast(patientReported.getPatientNameLast());
      patientMaster.setPatientNameFirst(patientReported.getPatientNameFirst());
      patientMaster.setPatientNameMiddle(patientReported.getPatientNameMiddle());
      patientMaster.setPatientBirthDate(patientReported.getPatientBirthDate());
      patientMaster.setPatientPhoneFrag(patientReported.getPatientPhone());
      patientMaster.setPatientAddressFrag(patientReported.getPatientAddressZip());
    }


  }

  /**
   * This methods create the patient resource based on the patientReported information
   * @param pr the patientReported
   * @return the Patient resource
   */
  public static Patient patientReportedToFhir(PatientReported pr) {
    return getFhirPatient(pr);
  }

  public static Patient getFhirPatient( PatientReported pr) {
    Patient p = new Patient();
	 p.setId(pr.getPatientReportedExternalLink());
    Identifier id = p.addIdentifier();
    id.setValue(pr.getPatientReportedExternalLink());
//    p.setId(pr.getPatientReportedExternalLink());

    HumanName name = p.addName();
    name.setFamily(pr.getPatientNameLast());
    name.addGivenElement().setValue(pr.getPatientNameFirst());
    name.addGivenElement().setValue(pr.getPatientNameMiddle());

    if (null != pr.getPatientEmail()) {
      p.addTelecom().setSystem(ContactPointSystem.EMAIL)
          .setValue(pr.getPatientEmail());
    }
    if (null != pr.getPatientPhone()) {
      p.addTelecom().setSystem(ContactPointSystem.PHONE)
          .setValue(pr.getPatientPhone());
    }
    switch (pr.getPatientSex()) {
      case "M":
        p.setGender(AdministrativeGender.MALE);
        break;
      case "F":
        p.setGender(AdministrativeGender.FEMALE);
        break;
      default:
        p.setGender(AdministrativeGender.OTHER);
    }
    p.setBirthDate(pr.getPatientBirthDate());
    if (null == pr.getPatientDeathDate()) {
      p.getDeceasedBooleanType().setValue(false);
    } else {
      p.getDeceasedBooleanType().setValue(true);
      p.getDeceasedDateTimeType().setValue(pr.getPatientDeathDate());
    }

    Address address = p.addAddress();
    address.addLine(pr.getPatientAddressLine1());
    address.addLine(pr.getPatientAddressLine2());
    address.setCity(pr.getPatientAddressCity());
    address.setCountry(pr.getPatientAddressCountry());
    address.setState(pr.getPatientAddressState());
    address.setPostalCode(pr.getPatientAddressZip());

    Patient.ContactComponent contact = p.addContact();
    HumanName contactName = new HumanName();
    contactName.setFamily(pr.getGuardianLast());
    contactName.addGivenElement().setValue(pr.getGuardianFirst());
    contactName.addGivenElement().setValue(pr.getGuardianMiddle());
    contact.setName(contactName);

    return p;
  }


  /**
   * This methods is looking for posssible matches based on the first name, last name between the provided patient
   * and the existing patients in the database
   * @param dataSession the Session
   * @param patient the patient
   * @return a list of PatientMaster who match the patient, null if none has been found
   */
  @SuppressWarnings("unchecked")
public static List<PatientMaster> findPossibleMatch(Session dataSession, Patient patient) {
    List<PatientMaster> matches;
    Query query = dataSession
        .createQuery("from PatientMaster where patientNameLast = ? and patientNameFirst= ? ");
    query.setParameter(0, patient.getNameFirstRep().getFamily());
    query.setParameter(1, patient.getNameFirstRep().getGiven().get(0).toString());
    //query.setParameter(2, patient.getBirthDate());
    matches = query.list();
    return matches;
  }

  /**
   * This methods is looking for matches based on the first name, last name and birthday between the provided patient
   * and the existing patients in the database
   * @param dataSession the Session
   * @param patient the patient
   * @return a list of PatientMaster who match the patient, null if none has been found
   */
  @SuppressWarnings("unchecked")
public static List<PatientMaster> findMatch(Session dataSession, Patient patient) {
    List<PatientMaster> matches;
    Query queryBigMatch = dataSession.createQuery(
        "from PatientMaster where patientNameLast = ? and patientNameFirst= ? and patientBirthDate = ?");
    queryBigMatch.setParameter(0, patient.getNameFirstRep().getFamily());
    queryBigMatch.setParameter(1, patient.getNameFirstRep().getGiven().get(0).toString());
    queryBigMatch.setParameter(2, patient.getBirthDate());
    matches = queryBigMatch.list();
    return matches;
  }


}
