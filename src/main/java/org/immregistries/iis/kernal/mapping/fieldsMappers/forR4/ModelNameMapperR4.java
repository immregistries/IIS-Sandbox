package org.immregistries.iis.kernal.mapping.fieldsMappers.forR4;

import org.hl7.fhir.r4.model.HumanName;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.fieldsMappers.IFieldMapper;
import org.immregistries.iis.kernal.mapping.fieldsMappers.ModelNameMapper;
import org.immregistries.iis.kernal.model.ModelName;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.resourceMappers.PatientMapper.V_2_NAME_TYPE_SYSTEM;

@Service
@Conditional(OnR4Condition.class)
public class ModelNameMapperR4 extends ModelNameMapper<HumanName> {

	@Override
	public Class<HumanName> fhirType() {
		return HumanName.class;
	}

	public org.hl7.fhir.r4.model.HumanName toFhir(ModelName modelName) {
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


	public ModelName localObject(org.hl7.fhir.r4.model.HumanName name) {
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


}
