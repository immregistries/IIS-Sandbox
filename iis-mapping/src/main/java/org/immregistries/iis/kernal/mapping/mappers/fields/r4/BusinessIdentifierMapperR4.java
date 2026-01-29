package org.immregistries.iis.kernal.mapping.mappers.fields.r4;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.immregistries.iis.fhir.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class BusinessIdentifierMapperR4 extends BusinessIdentifierMapper<Identifier> implements IR4FieldMapper<BusinessIdentifier, Identifier> {

	@Override
	public Class<Identifier> fhirType() {
		return Identifier.class;
	}

	public Identifier fhirObject(BusinessIdentifier businessIdentifier) {
		Identifier identifier = new Identifier()
			.setValue(businessIdentifier.getValue())
			.setSystem(businessIdentifier.getSystem());
		if (businessIdentifier.getType() != null) {
			identifier.setType(new CodeableConcept(
				new Coding(IDENTIFIER_TYPE_SYSTEM, businessIdentifier.getType(), "")));
		}
		return identifier;
	}

	public BusinessIdentifier localObject(Identifier identifier) {
		BusinessIdentifier businessIdentifier = new BusinessIdentifier();
		businessIdentifier.setSystem(identifier.getSystem());
		businessIdentifier.setValue(identifier.getValue());
		if (identifier.getType() != null && identifier.getType().hasCoding()) {
			businessIdentifier.setType(identifier.getType().getCodingFirstRep().getCode());
		}
		return businessIdentifier;
	}

}
