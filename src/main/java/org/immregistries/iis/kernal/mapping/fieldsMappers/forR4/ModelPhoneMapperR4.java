package org.immregistries.iis.kernal.mapping.fieldsMappers.forR4;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ContactPoint;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.fieldsMappers.ModelPhoneMapper;
import org.immregistries.iis.kernal.model.ModelPhone;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class ModelPhoneMapperR4 extends ModelPhoneMapper<ContactPoint> {

	@Override
	public Class<ContactPoint> fhirType() {
		return ContactPoint.class;
	}

    public ContactPoint fhirObject(ModelPhone modelPhone) {
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(modelPhone.getNumber());
        String use = modelPhone.getUse();
        if (use != null) {
            try {
                contactPoint.setUse(ContactPoint.ContactPointUse.fromCode(use));
            } catch (FHIRException ignored) {
                CodeMap codeMap = CodeMapManagerService.get().getCodeMap();
                Code useCode = codeMap.getCodeForCodeset(CodesetType.TELECOMMUNICATION_USE, use);
                if (useCode != null) {
                    contactPoint.addExtension(USE_EXTENSION_URL,
                            new org.hl7.fhir.r4.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
                    switch (use) {
                        case "": {
                            break;
                        }
                        case "PRN":
                        case "ORN":
                        case "VHN": {
                            contactPoint.setUse(ContactPoint.ContactPointUse.HOME);
                            break;
                        }
                        case "WPN": {
                            contactPoint.setUse(ContactPoint.ContactPointUse.WORK);
                            break;
                        }
                        case "PRS": {
                            contactPoint.setUse(ContactPoint.ContactPointUse.MOBILE);
                            break;
                        }
                    }
                }
            }
        }
        return contactPoint;
    }

    public ModelPhone localObject(ContactPoint contactPoint) {
        if (!contactPoint.getSystem().equals(ContactPoint.ContactPointSystem.PHONE)) {
            return null;
        } else {
            ModelPhone modelPhone = new ModelPhone();
            modelPhone.setNumber(contactPoint.getValue());
            org.hl7.fhir.r4.model.Extension useExtension = contactPoint.getExtensionByUrl(USE_EXTENSION_URL);
            if (useExtension != null) {
                org.hl7.fhir.r4.model.Coding coding = MappingHelper.extensionGetCoding(useExtension);
                if (coding != null && StringUtils.isNotBlank(coding.getCode())) {
                    modelPhone.setUse(coding.getCode());
                } else {
                    modelPhone.setUse("");
                }
            } else if (contactPoint.getUse() != null) {
                modelPhone.setUse(contactPoint.getUse().toCode());
            } else {
                modelPhone.setUse(null);
            }
            return modelPhone;
        }
    }
}
