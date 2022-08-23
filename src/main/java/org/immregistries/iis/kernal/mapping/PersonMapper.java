package org.immregistries.iis.kernal.mapping;


import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;

public class PersonMapper {

	public static final String ORGANISATION_ASSIGNING_AUTHORITY = "AssigningAuthority";
	public static final String PRACTITIONER = "Practitioner";


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
  public static ModelPerson getModelPerson(Practitioner practitioner) {
	  ModelPerson modelPerson = new ModelPerson();
	  modelPerson.setPersonId(practitioner.getId());
	  modelPerson.setPersonExternalLink(practitioner.getIdentifierFirstRep().getValue());
		if (practitioner.getNameFirstRep().getGiven().size() > 0) {
			modelPerson.setNameFirst(practitioner.getNameFirstRep().getGiven().get(0).getValue());
		}
		if (practitioner.getNameFirstRep().getGiven().size() > 1) {
			modelPerson.setNameMiddle(practitioner.getNameFirstRep().getGiven().get(1).getValue());
		}
		modelPerson.setNameLast(practitioner.getNameFirstRep().getFamily());
		modelPerson.setProfessionalSuffix(practitioner.getNameFirstRep().getSuffixAsSingleString());
		if (practitioner.getIdentifierFirstRep().getAssigner() != null) {
			modelPerson.setAssigningAuthority(practitioner.getIdentifierFirstRep().getAssigner().getReference());
		}

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

	public static Practitioner getFhirPractitioner(ModelPerson modelPerson) {
		Practitioner practitioner = new Practitioner();
		switch (new Reference(modelPerson.getIdentifierTypeCode()).getType()) {
			case "Organization": {
				practitioner.addIdentifier(MappingHelper.getFhirIdentifier(PRACTITIONER,modelPerson.getPersonExternalLink()).setAssigner(new Reference(modelPerson.getAssigningAuthority())));
				break;
			}
			case "System" : {
				practitioner.addIdentifier(MappingHelper.getFhirIdentifier(modelPerson.getIdentifierTypeCode(),modelPerson.getPersonExternalLink()));
				break;
			} default: {
				practitioner.addIdentifier(MappingHelper.getFhirIdentifier(PRACTITIONER,modelPerson.getPersonExternalLink()));
				break;
			}
		}

		HumanName name = practitioner.addName();
		name.setFamily(modelPerson.getNameLast());
		name.addGiven(modelPerson.getNameFirst());
		name.addGiven(modelPerson.getNameMiddle());
		switch (modelPerson.getNameTypeCode()) { // TODO map it more solidly
			case "A":
			case "S": {
				name.setUse(HumanName.NameUse.ANONYMOUS);
				break;
			}
			case "B":
			case "I":
			case "L": {
				name.setUse(HumanName.NameUse.OFFICIAL);
				break;
			}
			case "C":
			case "D": {
				name.setUse(HumanName.NameUse.USUAL);
				break;
			}
			case "M": {
				name.setUse(HumanName.NameUse.MAIDEN);
				break;
			}
			case "N": {
				name.setUse(HumanName.NameUse.NICKNAME);
				break;
			}
			case "P": {
				name.setUse(HumanName.NameUse.TEMP);
				break;
			}
			case "T":
			case "U": {
				name.setUse(HumanName.NameUse.NULL);
				break;
			}
		}
		if ( modelPerson.getProfessionalSuffix() != null) {
			name.addSuffix(modelPerson.getProfessionalSuffix());
		}
		return practitioner;
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

