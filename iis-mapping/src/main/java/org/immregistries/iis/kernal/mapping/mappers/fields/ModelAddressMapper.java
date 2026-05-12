package org.immregistries.iis.kernal.mapping.mappers.fields;

import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.ModelAddress;

public abstract class ModelAddressMapper<Address extends IBaseDatatype> implements IFieldMapper<ModelAddress, Address> {

	public static final String ADDRESS = "Address";

	@Override
	public Class<ModelAddress> localType() {
		return ModelAddress.class;
	}


	@Override
	public String fhirTypeName() {
		return ADDRESS;
	}

}
