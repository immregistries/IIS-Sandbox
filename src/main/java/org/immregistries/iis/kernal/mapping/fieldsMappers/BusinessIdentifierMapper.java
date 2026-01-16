package org.immregistries.iis.kernal.mapping.fieldsMappers;

import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.Identifier;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.springframework.stereotype.Service;

public abstract class BusinessIdentifierMapper<Identifier extends IBaseDatatype> implements IFieldMapper<BusinessIdentifier, Identifier> {
    public static final String IDENTIFIER_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";

	@Override
	public Class<BusinessIdentifier> localType() {
		return BusinessIdentifier.class;
	}


	@Override
	public String fhirTypeName() {
		return "Identifier";
	}

	/**
     * Converts to token param with System and value
     *
     * @return tokenParam
     */
    public TokenParam asTokenParam(BusinessIdentifier businessIdentifier) {
        TokenParam tokenParam = new TokenParam();
        if (StringUtils.isNotBlank(businessIdentifier.getValue())) {
            tokenParam.setValue(businessIdentifier.getValue());
            if (StringUtils.isNotBlank(businessIdentifier.getSystem())) {
                tokenParam.setSystem(businessIdentifier.getSystem());
            }
            // tokenParam.setModifier(TokenParamModifier.OF_TYPE).; TODO TYPE
            return tokenParam;
        }
        return null;
    }


}
