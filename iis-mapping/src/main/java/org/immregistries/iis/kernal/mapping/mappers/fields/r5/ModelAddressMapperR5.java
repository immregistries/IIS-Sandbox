package org.immregistries.iis.kernal.mapping.mappers.fields.r5;

import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Address;
import org.immregistries.iis.kernal.mapping.mappers.IR5Mapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.ModelAddressMapper;
import org.immregistries.iis.kernal.model.ModelAddress;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR5Condition.class)
public class ModelAddressMapperR5 extends ModelAddressMapper<Address> implements IR5Mapper<ModelAddress, Address> {


	@Override
	public Class<Address> fhirType() {
		return Address.class;
	}


	public Address fhirObject(ModelAddress modelAddress) {
        return new Address().addLine(modelAddress.getAddressLine1())
                .addLine(modelAddress.getAddressLine2())
                .setCity(modelAddress.getAddressCity())
                .setCountry(modelAddress.getAddressCountry())
                .setState(modelAddress.getAddressState())
                .setDistrict(modelAddress.getAddressCountyParish())
                .setPostalCode(modelAddress.getAddressZip());
    }

    public ModelAddress localObject(org.hl7.fhir.r5.model.Address address) {
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
