package org.immregistries.iis.kernal.mapping;


import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;

public class PersonMapper {

	public static final String ORGANISATION_ASSIGNING_AUTHORITY = "AssigningAuthority";


  public static ModelPerson getModelPerson(Person p) {
	  ModelPerson modelPerson = new ModelPerson();
	  modelPerson.setPersonId(p.getId());
	  modelPerson.setPersonExternalLink(p.getIdentifier().get(0).getValue());
		if (p.getNameFirstRep().getGiven().size() > 0) {
			modelPerson.setNameFirst(p.getNameFirstRep().getGiven().get(0).getValue());
		}
		if (p.getNameFirstRep().getGiven().size() > 1) {
			modelPerson.setNameMiddle(p.getNameFirstRep().getGiven().get(1).getValue());
		}
		modelPerson.setNameLast(p.getNameFirstRep().getFamily());
		modelPerson.setProfessionalSuffix(p.getNameFirstRep().getSuffixAsSingleString());
		modelPerson.setAssigningAuthority(p.getManagingOrganization().getIdentifier().getValue());

	  return modelPerson;
  }

	public static Person getFhirPerson(ModelPerson modelPerson) {
	   Person p = new Person();
	   p.setId(modelPerson.getPersonId());
	   p.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.PERSON, modelPerson.getPersonExternalLink()));
		HumanName name = p.addName();
		name.setFamily(modelPerson.getNameLast());
		name.addGiven(modelPerson.getNameFirst());
		name.addGiven(modelPerson.getNameMiddle());
		if ( modelPerson.getProfessionalSuffix() != null) {
			name.addSuffix(modelPerson.getProfessionalSuffix());
		}
		p.setManagingOrganization(MappingHelper.getFhirReference(MappingHelper.ORGANISATION,ORGANISATION_ASSIGNING_AUTHORITY, modelPerson.getAssigningAuthority()));
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
//	  p.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.PATIENT_REPORTED,pr.getPatientReportedExternalLink()));
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

