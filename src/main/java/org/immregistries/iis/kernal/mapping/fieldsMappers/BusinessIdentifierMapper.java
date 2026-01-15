package org.immregistries.iis.kernal.mapping.fieldsMappers;

import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.springframework.stereotype.Service;

@Service
public class BusinessIdentifierMapper implements IFieldMapper<BusinessIdentifier, Identifier, org.hl7.fhir.r5.model.Identifier> {
    public static final String IDENTIFIER_TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";

	@Override
	public Class<BusinessIdentifier> localType() {
		return BusinessIdentifier.class;
	}

	@Override
	public Class<org.hl7.fhir.r5.model.Identifier> r5Type() {
		return org.hl7.fhir.r5.model.Identifier.class;
	}

	@Override
	public Class<org.hl7.fhir.r4.model.Identifier> r4Type() {
		return org.hl7.fhir.r4.model.Identifier.class;
	}

    public org.hl7.fhir.r5.model.Identifier toR5(BusinessIdentifier businessIdentifier) {
        org.hl7.fhir.r5.model.Identifier identifier = new org.hl7.fhir.r5.model.Identifier()
                .setValue(businessIdentifier.getValue())
                .setSystem(businessIdentifier.getSystem());
        if (businessIdentifier.getType() != null) {
            identifier.setType(new org.hl7.fhir.r5.model.CodeableConcept(
                    new org.hl7.fhir.r5.model.Coding(IDENTIFIER_TYPE_SYSTEM, businessIdentifier.getType(), "")));
        }
        return identifier;
    }

    public org.hl7.fhir.r4.model.Identifier toR4(BusinessIdentifier businessIdentifier) {
        org.hl7.fhir.r4.model.Identifier identifier = new org.hl7.fhir.r4.model.Identifier()
                .setValue(businessIdentifier.getValue())
                .setSystem(businessIdentifier.getSystem());
        if (businessIdentifier.getType() != null) {
            identifier.setType(new org.hl7.fhir.r4.model.CodeableConcept(
                    new org.hl7.fhir.r4.model.Coding(IDENTIFIER_TYPE_SYSTEM, businessIdentifier.getType(), "")));
        }
        return identifier;
    }

    public BusinessIdentifier fromR5(org.hl7.fhir.r5.model.Identifier identifier) {
        BusinessIdentifier businessIdentifier = new BusinessIdentifier();
        businessIdentifier.setSystem(identifier.getSystem());
        businessIdentifier.setValue(identifier.getValue());
        if (identifier.getType() != null) {
            businessIdentifier.setType(identifier.getType().getCode(IDENTIFIER_TYPE_SYSTEM));
        }
        return businessIdentifier;
    }

    public BusinessIdentifier fromR4(org.hl7.fhir.r4.model.Identifier identifier) {
        BusinessIdentifier businessIdentifier = new BusinessIdentifier();
        businessIdentifier.setSystem(identifier.getSystem());
        businessIdentifier.setValue(identifier.getValue());
        if (identifier.getType() != null && identifier.getType().hasCoding()) {
            businessIdentifier.setType(identifier.getType().getCodingFirstRep().getCode());
        }
        return businessIdentifier;
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
