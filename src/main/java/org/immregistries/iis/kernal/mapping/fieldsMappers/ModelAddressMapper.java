package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.ModelAddress;

public abstract class ModelAddressMapper<Address extends IBaseDatatype> implements IFieldMapper<ModelAddress, Address> {

	@Override
	public Class<ModelAddress> localType() {
		return ModelAddress.class;
	}


	@Override
	public String fhirTypeName() {
		return "Address";
	}

}
