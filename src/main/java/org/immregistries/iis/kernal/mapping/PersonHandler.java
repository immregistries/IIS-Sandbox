package org.immregistries.iis.kernal.mapping;


import org.hl7.fhir.r5.model.*;

public class PersonHandler {



  public static org.immregistries.iis.kernal.model.Person getModelPerson(Person p) {
	  org.immregistries.iis.kernal.model.Person person = new org.immregistries.iis.kernal.model.Person();
	  person.setPersonId(p.getId());
	  person.setPersonExternalLink(p.getIdentifier().get(0).getValue());
		if (p.getNameFirstRep().getGiven().size() > 0) {
			person.setNameFirst(p.getNameFirstRep().getGiven().get(0).getValue());
		}
		if (p.getNameFirstRep().getGiven().size() > 1) {
			person.setNameMiddle(p.getNameFirstRep().getGiven().get(1).getValue());
		}
		person.setNameLast(p.getNameFirstRep().getFamily());
		person.setProfessionalSuffix(p.getNameFirstRep().getSuffixAsSingleString());
		person.setAssigningAuthority(p.getManagingOrganization().getIdentifier().getValue());

	  return person;
  }

	public static Person getFhirPerson(org.immregistries.iis.kernal.model.Person dbPerson) {
	   Person p = new Person();
	   p.setId(dbPerson.getPersonId());
	   p.addIdentifier(MappingHelper.getFhirIdentifier("Person",dbPerson.getPersonExternalLink()));
		HumanName name = p.addName();
		name.setFamily(dbPerson.getNameLast());
		name.addGiven(dbPerson.getNameFirst());
		name.addGiven(dbPerson.getNameMiddle());
		if ( dbPerson.getProfessionalSuffix() != null) {
			name.addSuffix(dbPerson.getProfessionalSuffix());
		}
		p.setManagingOrganization(MappingHelper.getFhirReference("Organisation","AssigningAuthority", dbPerson.getAssigningAuthority()));
		return p;
	}


//  /** Outdated
//   * This method recreate the Hapi Person resource from the database information
//   * @param pr the patientReported found in the database
//   * @return Fhir Person resource
//   */
//  public static Person getFhirPerson(PatientReported pr) {
//    Person p = new Person();
//	  p.setId(pr.getPatientReportedExternalLink());
//	  p.addIdentifier(MappingHelper.getFhirIdentifier("PatientReported",pr.getPatientReportedExternalLink()));
//
//    HumanName name = p.addName();
//    name.setFamily(pr.getPatientNameLast());
//    name.addGivenElement().setValue(pr.getPatientNameFirst());
//    name.addGivenElement().setValue(pr.getPatientNameMiddle());
//
//    if (null != pr.getPatientEmail()) {
//      p.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL)
//          .setValue(pr.getPatientEmail());
//    }
//    if (null != pr.getPatientPhone()) {
//      p.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE)
//          .setValue(pr.getPatientPhone());
//    }
//    switch (pr.getPatientSex()) {
//      case "M":
//        p.setGender(Enumerations.AdministrativeGender.MALE);
//        break;
//      case "F":
//        p.setGender(Enumerations.AdministrativeGender.FEMALE);
//        break;
//      default:
//        p.setGender(Enumerations.AdministrativeGender.OTHER);
//    }
//    p.setBirthDate(pr.getPatientBirthDate());
//
//
//    Address address = p.addAddress();
//    address.addLine(pr.getPatientAddressLine1());
//    address.addLine(pr.getPatientAddressLine2());
//    address.setCity(pr.getPatientAddressCity());
//    address.setCountry(pr.getPatientAddressCountry());
//    address.setState(pr.getPatientAddressState());
//    address.setPostalCode(pr.getPatientAddressZip());
//
//    return p;
//  }
}

