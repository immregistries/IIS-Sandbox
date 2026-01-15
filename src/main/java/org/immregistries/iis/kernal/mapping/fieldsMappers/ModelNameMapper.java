package org.immregistries.iis.kernal.mapping.fieldsMappers;

import org.hl7.fhir.r4.model.HumanName;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ModelName;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE_SYSTEM;

@Service
public class ModelNameMapper implements IFieldMapper<ModelName, HumanName, org.hl7.fhir.r5.model.HumanName> {

	@Override
	public Class<ModelName> localType() {
		return ModelName.class;
	}

	@Override
	public Class<org.hl7.fhir.r5.model.HumanName> r5Type() {
		return org.hl7.fhir.r5.model.HumanName.class;
	}

	@Override
	public Class<org.hl7.fhir.r4.model.HumanName> r4Type() {
		return org.hl7.fhir.r4.model.HumanName.class;
	}

	public org.hl7.fhir.r4.model.HumanName toR4(ModelName modelName) {
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

	public org.hl7.fhir.r5.model.HumanName toR5(ModelName modelName) {
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

	public ModelName fromR4(org.hl7.fhir.r4.model.HumanName name) {
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

	public ModelName fromR5(org.hl7.fhir.r5.model.HumanName name) {
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
