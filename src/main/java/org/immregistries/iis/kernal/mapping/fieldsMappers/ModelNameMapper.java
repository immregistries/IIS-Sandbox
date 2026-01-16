package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.HumanName;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelName;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE_SYSTEM;

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
