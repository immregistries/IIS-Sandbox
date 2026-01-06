package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelName;
import org.immregistries.iis.kernal.model.VaccinationMaster;

import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE_SYSTEM;

public class ModelNameMapper implements FieldMapper<ModelName> {

	public Class<ModelName> localType() {
		return ModelName.class;
	}

	static org.hl7.fhir.r4.model.HumanName toR4(ModelName modelName) {
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

	static org.hl7.fhir.r5.model.HumanName toR5(ModelName modelName) {
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

	static ModelName fromR4(org.hl7.fhir.r4.model.HumanName name) {
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

	static ModelName fromR5(org.hl7.fhir.r5.model.HumanName name) {
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
