package org.immregistries.iis.kernal.mapping.mappers.fields.r5;

import org.hl7.fhir.r5.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.IisReferenceMapper;
import org.immregistries.iis.kernal.model.IisReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class IisReferenceMapperR5 extends IisReferenceMapper<Reference> {

	@Autowired
	private BusinessIdentifierMapperR5 businessIdentifierMapperR5;

	@Override
	public Class<Reference> fhirType() {
		return Reference.class;
	}

	@Override
	public Reference fhirObject(IisReference localField) {
		return new Reference(localField.getReference()).setIdentifier(businessIdentifierMapperR5.fhirObject(localField.getIdentifier()));
	}

	@Override
	public IisReference localObject(Reference reference) {
		if (reference == null) {
			return null;
		}
		IisReference iisReference = new IisReference();
		iisReference.setIdentifier(businessIdentifierMapperR5.localObject(reference.getIdentifier()));
		iisReference.setReference(reference.getReference());
		return iisReference;
	}
}
