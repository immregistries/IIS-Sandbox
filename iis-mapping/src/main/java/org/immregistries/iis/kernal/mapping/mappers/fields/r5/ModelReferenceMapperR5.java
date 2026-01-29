package org.immregistries.iis.kernal.mapping.mappers.fields.r5;

import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.mappers.IR5Mapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.ModelReferenceMapper;
import org.immregistries.iis.kernal.model.ModelReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class ModelReferenceMapperR5 extends ModelReferenceMapper<Reference> implements IR5Mapper<ModelReference, Reference> {

	@Autowired
	private org.immregistries.iis.kernal.mapping.mappers.fields.r5.BusinessIdentifierMapperR5 businessIdentifierMapperR5;

	@Override
	public Class<Reference> fhirType() {
		return Reference.class;
	}

	@Override
	public Reference fhirObject(ModelReference localField) {
		return new Reference(localField.getReference()).setIdentifier(businessIdentifierMapperR5.fhirObject(localField.getIdentifier()));
	}

	@Override
	public ModelReference localObject(Reference reference) {
		if (reference == null) {
			return null;
		}
		ModelReference modelReference = new ModelReference();
		modelReference.setIdentifier(businessIdentifierMapperR5.localObject(reference.getIdentifier()));
		modelReference.setReference(reference.getReference());
		return modelReference;
	}
}
