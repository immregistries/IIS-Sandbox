package org.immregistries.iis.kernal.mapping.mappers.fields.r4;

import org.hl7.fhir.r4.model.Identifier;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class BusinessIdentifierMapperR4 extends BusinessIdentifierMapper<Identifier> {

	@Override
	public Class<org.hl7.fhir.r4.model.Identifier> fhirType() {
		return org.hl7.fhir.r4.model.Identifier.class;
	}

	public org.hl7.fhir.r4.model.Identifier fhirObject(BusinessIdentifier businessIdentifier) {
		org.hl7.fhir.r4.model.Identifier identifier = new org.hl7.fhir.r4.model.Identifier()
			.setValue(businessIdentifier.getValue())
			.setSystem(businessIdentifier.getSystem());
		if (businessIdentifier.getType() != null) {
			identifier.setType(new org.hl7.fhir.r4.model.CodeableConcept(
				new org.hl7.fhir.r4.model.Coding(IDENTIFIER_TYPE_SYSTEM, businessIdentifier.getType(), "")));
		}
		return identifier;
	}

	public BusinessIdentifier localObject(org.hl7.fhir.r4.model.Identifier identifier) {
		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setSystem(identifier.getSystem());
		businessIdentifier.setValue(identifier.getValue());
		if (identifier.getType() != null && identifier.getType().hasCoding()) {
			businessIdentifier.setType(identifier.getType().getCodingFirstRep().getCode());
		}
		return businessIdentifier;
	}

}
