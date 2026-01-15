package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.immregistries.iis.kernal.model.ModelAddress;
import org.springframework.stereotype.Service;

@Service
public class ModelAddressMapper implements IFieldMapper<ModelAddress, Address, org.hl7.fhir.r5.model.Address> {

	@Override
	public Class<ModelAddress> localType() {
		return ModelAddress.class;
	}

	@Override
	public Class<org.hl7.fhir.r5.model.Address> r5Type() {
		return org.hl7.fhir.r5.model.Address.class;
	}

	@Override
	public Class<org.hl7.fhir.r4.model.Address> r4Type() {
		return org.hl7.fhir.r4.model.Address.class;
	}

	@Override
	public String fhirType() {
		return "Address";
	}

	public org.hl7.fhir.r4.model.Address toR4(ModelAddress modelAddress) {
        return new org.hl7.fhir.r4.model.Address().addLine(modelAddress.getAddressLine1())
                .addLine(modelAddress.getAddressLine2())
                .setCity(modelAddress.getAddressCity())
                .setCountry(modelAddress.getAddressCountry())
                .setState(modelAddress.getAddressState())
                .setDistrict(modelAddress.getAddressCountyParish())
                .setPostalCode(modelAddress.getAddressZip());
    }


	public org.hl7.fhir.r5.model.Address toR5(ModelAddress modelAddress) {
        return new org.hl7.fhir.r5.model.Address().addLine(modelAddress.getAddressLine1())
                .addLine(modelAddress.getAddressLine2())
                .setCity(modelAddress.getAddressCity())
                .setCountry(modelAddress.getAddressCountry())
                .setState(modelAddress.getAddressState())
                .setDistrict(modelAddress.getAddressCountyParish())
                .setPostalCode(modelAddress.getAddressZip());
    }

    public ModelAddress fromR4(org.hl7.fhir.r4.model.Address address) {
        ModelAddress modelAddress = new ModelAddress();
        if (!address.getLine().isEmpty()) {
            modelAddress.setAddressLine1(address.getLine().get(0).getValueNotNull());
        }
        if (address.getLine().size() > 1) {
            modelAddress.setAddressLine2(address.getLine().get(1).getValueNotNull());
        }
        modelAddress.setAddressCity(StringUtils.defaultString(address.getCity()));
        modelAddress.setAddressState(StringUtils.defaultString(address.getState()));
        modelAddress.setAddressZip(StringUtils.defaultString(address.getPostalCode()));
        modelAddress.setAddressCountry(StringUtils.defaultString(address.getCountry()));
        modelAddress.setAddressCountyParish(StringUtils.defaultString(address.getDistrict()));
        return modelAddress;
    }

    public ModelAddress fromR5(org.hl7.fhir.r5.model.Address address) {
        ModelAddress modelAddress = new ModelAddress();
        if (!address.getLine().isEmpty()) {
            modelAddress.setAddressLine1(address.getLine().get(0).getValueNotNull());
        }
        if (address.getLine().size() > 1) {
            modelAddress.setAddressLine2(address.getLine().get(1).getValueNotNull());
        }
        modelAddress.setAddressCity(StringUtils.defaultString(address.getCity()));
        modelAddress.setAddressState(StringUtils.defaultString(address.getState()));
        modelAddress.setAddressZip(StringUtils.defaultString(address.getPostalCode()));
        modelAddress.setAddressCountry(StringUtils.defaultString(address.getCountry()));
        modelAddress.setAddressCountyParish(StringUtils.defaultString(address.getDistrict()));
        return modelAddress;
    }
}
