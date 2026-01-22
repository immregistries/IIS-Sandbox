package org.immregistries.iis.kernal.mapping.mappers.fields;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.IisReference;

public abstract class IisReferenceMapper<Reference extends IBaseDatatype> implements IFieldMapper<IisReference, Reference> {

	@Override
	public Class<IisReference> localType() {
		return IisReference.class;
	}

	@Override
	public String fhirTypeName() {
		return "Reference";
	}
}
