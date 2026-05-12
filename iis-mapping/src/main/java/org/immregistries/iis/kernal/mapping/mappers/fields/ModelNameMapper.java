package org.immregistries.iis.kernal.mapping.mappers.fields;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.ModelName;

public abstract class ModelNameMapper<HumanName extends IBaseDatatype> implements IFieldMapper<ModelName, HumanName> {

	public static final String HUMAN_NAME = "HumanName";

	@Override
	public Class<ModelName> localType() {
		return ModelName.class;
	}

	@Override
	public String fhirTypeName() {
		return HUMAN_NAME;
	}

}
