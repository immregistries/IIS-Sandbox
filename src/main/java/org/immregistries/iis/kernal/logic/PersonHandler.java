package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Person;
import org.immregistries.iis.kernal.model.PatientReported;

public class PersonHandler {
  public static Person getPerson(PatientReported pr) {

    Person p = new Person();
    Identifier id = p.addIdentifier();
    id.setValue(pr.getPatientReportedExternalLink());
    p.setId(pr.getPatientReportedExternalLink());

    HumanName name = p.addName();
    name.setFamily(pr.getPatientNameLast());
    name.addGivenElement().setValue(pr.getPatientNameFirst());
    name.addGivenElement().setValue(pr.getPatientNameMiddle());

    if (null != pr.getPatientEmail()) {
      p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL)
          .setValue(pr.getPatientEmail());
    }
    if (null != pr.getPatientPhone()) {
      p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE)
          .setValue(pr.getPatientPhone());
    }
    switch (pr.getPatientSex()) {
      case "M":
        p.setGender(Enumerations.AdministrativeGender.MALE);
        break;
      case "F":
        p.setGender(Enumerations.AdministrativeGender.FEMALE);
        break;
      default:
        p.setGender(Enumerations.AdministrativeGender.OTHER);
    }
    p.setBirthDate(pr.getPatientBirthDate());


    Address address = p.addAddress();
    address.addLine(pr.getPatientAddressLine1());
    address.addLine(pr.getPatientAddressLine2());
    address.setCity(pr.getPatientAddressCity());
    address.setCountry(pr.getPatientAddressCountry());
    address.setState(pr.getPatientAddressState());
    address.setPostalCode(pr.getPatientAddressZip());

    return p;
  }
}
