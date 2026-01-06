package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelPhone;

public class ModelPhoneMapper {
    public static final String PHONE_USE_V2_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0201";
    public static final String USE_EXTENSION_URL = "use";

    public static org.hl7.fhir.r4.model.ContactPoint toR4(ModelPhone modelPhone) {
        org.hl7.fhir.r4.model.ContactPoint contactPoint = new org.hl7.fhir.r4.model.ContactPoint();
        contactPoint.setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
                .setValue(modelPhone.getNumber());
        String use = modelPhone.getUse();
        if (use != null) {
            try {
                contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.fromCode(use));
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
                            contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.HOME);
                            break;
                        }
                        case "WPN": {
                            contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.WORK);
                            break;
                        }
                        case "PRS": {
                            contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.MOBILE);
                            break;
                        }
                    }
                }
            }
        }
        return contactPoint;
    }

    public static org.hl7.fhir.r5.model.ContactPoint toR5(ModelPhone modelPhone) {
        org.hl7.fhir.r5.model.ContactPoint contactPoint = new org.hl7.fhir.r5.model.ContactPoint();
        contactPoint.setSystem(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)
                .setValue(modelPhone.getNumber());
        String use = modelPhone.getUse();
        if (use != null) {
            try {
                contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.fromCode(use));
            } catch (FHIRException ignored) {
                CodeMap codeMap = CodeMapManagerService.get().getCodeMap();
                Code useCode = codeMap.getCodeForCodeset(CodesetType.TELECOMMUNICATION_USE, use);
                if (useCode != null) {
                    contactPoint.addExtension(USE_EXTENSION_URL,
                            new org.hl7.fhir.r5.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
                    switch (use) {
                        case "": {
                            break;
                        }
                        case "PRN":
                        case "ORN":
                        case "VHN": {
                            contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.HOME);
                            break;
                        }
                        case "WPN": {
                            contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.WORK);
                            break;
                        }
                        case "PRS": {
                            contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.MOBILE);
                            break;
                        }
                    }
                }
            }
        }
        return contactPoint;
    }

    public static ModelPhone fromR4(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
        if (!contactPoint.getSystem().equals(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)) {
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

    public static ModelPhone fromR5(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
        if (!contactPoint.getSystem().equals(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)) {
            return null;
        } else {
            ModelPhone modelPhone = new ModelPhone();
            modelPhone.setNumber(contactPoint.getValue());
            org.hl7.fhir.r5.model.Extension useExtension = contactPoint.getExtensionByUrl(USE_EXTENSION_URL);
            if (useExtension != null) {
                org.hl7.fhir.r5.model.Coding coding = MappingHelper.extensionGetCoding(useExtension);
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

    public static ModelPhone fromFhir(org.hl7.fhir.instance.model.api.ICompositeType contactPoint) {
        if (contactPoint instanceof org.hl7.fhir.r5.model.ContactPoint) {
            return fromR5((org.hl7.fhir.r5.model.ContactPoint) contactPoint);
        } else if (contactPoint instanceof org.hl7.fhir.r4.model.ContactPoint) {
            return fromR4((org.hl7.fhir.r4.model.ContactPoint) contactPoint);
        } else {
            return null;
        }
    }
}
