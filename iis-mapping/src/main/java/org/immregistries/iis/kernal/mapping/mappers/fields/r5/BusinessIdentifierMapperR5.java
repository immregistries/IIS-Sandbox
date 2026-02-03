package org.immregistries.iis.kernal.mapping.mappers.fields.r5;

import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import org.hl7.fhir.r5.model.Identifier;
import org.immregistries.iis.kernal.mapping.mappers.IR5Mapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class BusinessIdentifierMapperR5 extends BusinessIdentifierMapper<Identifier> implements IR5Mapper<BusinessIdentifier, Identifier> {

	@Override
	public Class<Identifier> fhirType() {
		return org.hl7.fhir.r5.model.Identifier.class;
	}

	public org.hl7.fhir.r5.model.Identifier fhirObject(BusinessIdentifier businessIdentifier) {
		org.hl7.fhir.r5.model.Identifier identifier = new org.hl7.fhir.r5.model.Identifier()
			.setValue(businessIdentifier.getValue())
			.setSystem(businessIdentifier.getSystem());
		if (businessIdentifier.getType() != null) {
			identifier.setType(new org.hl7.fhir.r5.model.CodeableConcept(
				new org.hl7.fhir.r5.model.Coding(IDENTIFIER_TYPE_SYSTEM, businessIdentifier.getType(), "")));
		}
		return identifier;
	}


	public BusinessIdentifier localObject(org.hl7.fhir.r5.model.Identifier identifier) {
		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setSystem(identifier.getSystem());
		businessIdentifier.setValue(identifier.getValue());
		if (identifier.getType() != null) {
			businessIdentifier.setType(identifier.getType().getCode(IDENTIFIER_TYPE_SYSTEM));
		}
		return businessIdentifier;
	}



}
