package org.immregistries.iis.kernal.mapping.mappers.fields.r4;

import org.hl7.fhir.r4.model.Reference;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.IisReferenceMapper;
import org.immregistries.iis.kernal.model.IisReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class IisReferenceMapperR4 extends IisReferenceMapper<Reference> {

	@Autowired
	private BusinessIdentifierMapperR4 businessIdentifierMapperR4;

	@Override
	public Class<Reference> fhirType() {
		return Reference.class;
	}

	@Override
	public Reference fhirObject(IisReference localField) {
		return new Reference(localField.getReference()).setIdentifier(businessIdentifierMapperR4.fhirObject(localField.getIdentifier()));
	}

	@Override
	public IisReference localObject(Reference reference) {
		IisReference iisReference = new IisReference();
		iisReference.setIdentifier(businessIdentifierMapperR4.localObject(reference.getIdentifier()));
		iisReference.setReference(reference.getReference());
		return iisReference;
	}
}
