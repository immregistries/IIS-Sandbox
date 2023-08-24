package org.immregistries.iis.kernal.mapping.forR5;

import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedPerson;
import org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper;
import org.immregistries.iis.kernal.mapping.Interfaces.RelatedPersonMapper;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class RelatedPersonMapperR5 implements RelatedPersonMapper<RelatedPerson> {
	public void fillGuardianInformation(PatientMaster patientMaster, RelatedPerson relatedPerson){
		patientMaster.setGuardianLast(relatedPerson.getNameFirstRep().getFamily());
		if (relatedPerson.getNameFirstRep().getGiven().size() > 0) {
			patientMaster.setGuardianFirst(relatedPerson.getNameFirstRep().getGiven().get(0).getValueNotNull());
		}
		if (relatedPerson.getNameFirstRep().getGiven().size() > 1) {
			patientMaster.setGuardianMiddle(relatedPerson.getNameFirstRep().getGiven().get(1).getValueNotNull());
		}
		patientMaster.setGuardianRelationship(relatedPerson.getRelationshipFirstRep().getCodingFirstRep().getCode());
	}

	public RelatedPerson getFhirRelatedPersonFromPatient(PatientMaster patientMaster){
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setPatient(new Reference("Patient/" + patientMaster.getPatientId()));
		relatedPerson.addRelationship().addCoding().setSystem("").setCode(patientMaster.getGuardianRelationship());
		HumanName name = relatedPerson.addName();
		name.setFamily(patientMaster.getGuardianLast());
		name.addGivenElement().setValue(patientMaster.getGuardianFirst());
		name.addGivenElement().setValue(patientMaster.getGuardianMiddle());
		return relatedPerson;
	}
}
