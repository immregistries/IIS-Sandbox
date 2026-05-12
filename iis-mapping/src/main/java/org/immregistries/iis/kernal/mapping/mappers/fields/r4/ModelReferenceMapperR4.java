package org.immregistries.iis.kernal.mapping.mappers.fields.r4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.mapping.mappers.fields.ModelReferenceMapper;
import org.immregistries.iis.kernal.model.ModelReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class ModelReferenceMapperR4 extends ModelReferenceMapper<Reference> implements IR4FieldMapper<ModelReference, Reference> {

	@Autowired
	private org.immregistries.iis.kernal.mapping.mappers.fields.r4.BusinessIdentifierMapperR4 businessIdentifierMapperR4;

	@Override
	public Class<Reference> fhirType() {
		return Reference.class;
	}

	@Override
	public Reference fhirObject(ModelReference localField) {
		return new Reference(localField.getReference()).setIdentifier(businessIdentifierMapperR4.fhirObject(localField.getIdentifier()));
	}

	@Override
	public ModelReference localObject(Reference reference) {
		ModelReference modelReference = new ModelReference();
		modelReference.setIdentifier(businessIdentifierMapperR4.localObject(reference.getIdentifier()));
		modelReference.setReference(reference.getReference());
		return modelReference;
	}
}
