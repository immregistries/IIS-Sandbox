package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelName;

import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.V_2_NAME_TYPE_SYSTEM;

public class ModelNameMapper {

    public static org.hl7.fhir.r4.model.HumanName toR4(ModelName modelName) {
        org.hl7.fhir.r4.model.HumanName name = new org.hl7.fhir.r4.model.HumanName()
                .setFamily(modelName.getNameLast())
                .addGiven(modelName.getNameFirst())
                .addGiven(modelName.getNameMiddle());
        if (modelName.getNameType() != null) {
            name.addExtension().setUrl(V_2_NAME_TYPE)
                    .setValue(new org.hl7.fhir.r4.model.Coding(V_2_NAME_TYPE_SYSTEM, modelName.getNameType(), ""));
        }
        return name;
    }

    public static org.hl7.fhir.r5.model.HumanName toR5(ModelName modelName) {
        org.hl7.fhir.r5.model.HumanName name = new org.hl7.fhir.r5.model.HumanName()
                .setFamily(modelName.getNameLast())
                .addGiven(modelName.getNameFirst())
                .addGiven(modelName.getNameMiddle());
        if (modelName.getNameType() != null) {
            name.addExtension().setUrl(V_2_NAME_TYPE)
                    .setValue(new org.hl7.fhir.r5.model.Coding(V_2_NAME_TYPE_SYSTEM, modelName.getNameType(), ""));
        }
        return name;
    }

    public static ModelName fromR4(org.hl7.fhir.r4.model.HumanName name) {
        ModelName modelName = new ModelName();
        modelName.setNameLast(name.getFamily());
        if (!name.getGiven().isEmpty()) {
            modelName.setNameFirst(name.getGiven().get(0).getValueNotNull());
        }
        if (name.getGiven().size() > 1) {
            modelName.setNameMiddle(name.getGiven().get(1).getValueNotNull());
        }
        org.hl7.fhir.r4.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
        if (nameType != null) {
            org.hl7.fhir.r4.model.Coding coding = MappingHelper.extensionGetCoding(nameType);
            if (coding != null && coding.hasCode()) {
                modelName.setNameType(coding.getCode());
            } else {
                modelName.setNameType("");
            }
        } else {
            modelName.setNameType(null);
        }
        return modelName;
    }

    public static ModelName fromR5(org.hl7.fhir.r5.model.HumanName name) {
        ModelName modelName = new ModelName();
        modelName.setNameLast(name.getFamily());
        if (!name.getGiven().isEmpty()) {
            modelName.setNameFirst(name.getGiven().get(0).getValueNotNull());
        }
        if (name.getGiven().size() > 1) {
            modelName.setNameMiddle(name.getGiven().get(1).getValueNotNull());
        }
        org.hl7.fhir.r5.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
        if (nameType != null) {
            org.hl7.fhir.r5.model.Coding coding = MappingHelper.extensionGetCoding(nameType);
            if (coding != null && coding.hasCode()) {
                modelName.setNameType(coding.getCode());
            } else {
                modelName.setNameType("");
            }
        } else {
            modelName.setNameType(null);
        }
        return modelName;
    }
}
