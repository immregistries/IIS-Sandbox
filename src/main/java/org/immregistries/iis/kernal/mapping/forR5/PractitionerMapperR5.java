package org.immregistries.iis.kernal.mapping.forR5;


import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.PractitionerMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class PractitionerMapperR5 implements PractitionerMapper<Practitioner> {



  public ModelPerson getModelPerson(Practitioner practitioner) {
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

	public Practitioner getFhirResource(ModelPerson modelPerson) {
		Practitioner practitioner = new Practitioner();
		try {
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
		} catch (NullPointerException e) { // If typecode is not reference
			practitioner.addIdentifier(MappingHelper.getFhirIdentifier(modelPerson.getIdentifierTypeCode(),modelPerson.getPersonExternalLink()));
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

}
