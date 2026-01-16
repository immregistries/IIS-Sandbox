package org.immregistries.iis.kernal.mapping.mappers.fields;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.ModelPhone;

public abstract class ModelPhoneMapper<ContactPoint extends IBaseDatatype> implements IFieldMapper<ModelPhone, ContactPoint> {
    public static final String PHONE_USE_V2_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0201";
    public static final String USE_EXTENSION_URL = "use";

	@Override
	public Class<ModelPhone> localType() {
		return ModelPhone.class;
	}


	@Override
	public String fhirTypeName() {
		return "ContactPoint";
	}



}
