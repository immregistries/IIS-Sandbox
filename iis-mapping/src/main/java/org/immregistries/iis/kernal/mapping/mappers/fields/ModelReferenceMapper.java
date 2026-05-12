package org.immregistries.iis.kernal.mapping.mappers.fields;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.ModelReference;

public abstract class ModelReferenceMapper<Reference extends IBaseDatatype> implements IFieldMapper<ModelReference, Reference> {

	@Override
	public Class<ModelReference> localType() {
		return ModelReference.class;
	}

	@Override
	public String fhirTypeName() {
		return "Reference";
	}
}
