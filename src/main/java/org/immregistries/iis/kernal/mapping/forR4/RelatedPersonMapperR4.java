package org.immregistries.iis.kernal.mapping.forR4;

import org.hl7.fhir.r4.model.RelatedPerson;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.interfaces.RelatedPersonMapper;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class RelatedPersonMapperR4 implements RelatedPersonMapper<RelatedPerson> {
//	public void fillGuardianInformation(PatientMaster patientMaster, RelatedPerson relatedPerson){
//		PatientGuardian patientGuardian = new PatientGuardian();
//		patientMaster.addPatientGuardian(patientGuardian);
//		patientGuardian.setGuardianLast(relatedPerson.getNameFirstRep().getFamily());
//		if (relatedPerson.getNameFirstRep().getGiven().size() > 0) {
//			patientGuardian.setGuardianFirst(relatedPerson.getNameFirstRep().getGiven().get(0).getValueNotNull());
//		}
//		if (relatedPerson.getNameFirstRep().getGiven().size() > 1) {
//			patientGuardian.setGuardianMiddle(relatedPerson.getNameFirstRep().getGiven().get(1).getValueNotNull());
//		}
//		patientGuardian.setGuardianRelationship(relatedPerson.getRelationshipFirstRep().getCodingFirstRep().getCode());
//	}
//
//	public RelatedPerson getFhirRelatedPersonFromPatient(PatientMaster patientMaster){ // TODO change this mapping ?
//		RelatedPerson relatedPerson = new RelatedPerson();
//		relatedPerson.setPatient(new Reference("Patient/" + patientMaster.getPatientId()));
//		relatedPerson.addRelationship().addCoding().setSystem("").setCode(patientMaster.getGuardianRelationship());
//		HumanName name = relatedPerson.addName();
//		name.setFamily(patientMaster.getGuardianLast());
//		name.addGivenElement().setValue(patientMaster.getGuardianFirst());
//		name.addGivenElement().setValue(patientMaster.getGuardianMiddle());
//		return relatedPerson;
//	}
}
