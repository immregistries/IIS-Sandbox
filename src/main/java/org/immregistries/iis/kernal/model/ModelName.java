package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.mapping.MappingHelper;

import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.V_2_NAME_TYPE_SYSTEM;

public class ModelName extends AbstractDiffable<ModelName> {
	private String nameLast = "";
	private String nameFirst = "";
	private String nameMiddle = "";
	private String nameType = "";

	public ModelName(String nameLast, String nameFirst, String nameMiddle, String nameType) {
		this.nameLast = nameLast;
		this.nameFirst = nameFirst;
		this.nameMiddle = nameMiddle;
		this.nameType = nameType;
	}

	private ModelName(org.hl7.fhir.r4.model.HumanName name) {
		this.setNameLast(name.getFamily());
		if (!name.getGiven().isEmpty()) {
			this.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			this.setNameMiddle(name.getGiven().get(1).getValueNotNull());
		}
		org.hl7.fhir.r4.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
		if (nameType != null) {
			org.hl7.fhir.r4.model.Coding coding = MappingHelper.extensionGetCoding(nameType);
			if (coding != null && StringUtils.isNotBlank(coding.getCode())) {
				this.setNameType(coding.getCode());
			} else {
				this.setNameType("");
			}
		} else {
			this.setNameType(null);
		}
	}

	private ModelName(org.hl7.fhir.r5.model.HumanName name) {
		this.setNameLast(name.getFamily());
		if (!name.getGiven().isEmpty()) {
			this.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			this.setNameMiddle(name.getGiven().get(1).getValueNotNull());
		}
		org.hl7.fhir.r5.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
		if (nameType != null) {
			org.hl7.fhir.r5.model.Coding coding = MappingHelper.extensionGetCoding(nameType);
			if (coding != null && StringUtils.isNotBlank(coding.getCode())) {
				this.setNameType(coding.getCode());
			} else {
				this.setNameType("");
			}
		} else {
			this.setNameType(null);
		}
	}

	public ModelName() {
	}

	public String getNameLast() {
		return nameLast;
	}

	public void setNameLast(String nameLast) {
		this.nameLast = nameLast;
	}

	public String getNameFirst() {
		return nameFirst;
	}

	public void setNameFirst(String nameFirst) {
		this.nameFirst = nameFirst;
	}

	public String getNameMiddle() {
		return nameMiddle;
	}

	public void setNameMiddle(String nameMiddle) {
		this.nameMiddle = nameMiddle;
	}

	public String getNameType() {
		return nameType;
	}

	public void setNameType(String nameType) {
		this.nameType = nameType;
	}

	public org.hl7.fhir.r4.model.HumanName toR4() {
		org.hl7.fhir.r4.model.HumanName name = new org.hl7.fhir.r4.model.HumanName()
			.setFamily(this.getNameLast())
			.addGiven(this.getNameFirst())
			.addGiven(this.getNameMiddle());
		if (this.getNameType() != null) {
			name.addExtension().setUrl(V_2_NAME_TYPE).setValue(new org.hl7.fhir.r4.model.Coding(V_2_NAME_TYPE_SYSTEM, this.getNameType(), ""));
		}
		return name;
	}

	public org.hl7.fhir.r5.model.HumanName toR5() {
		org.hl7.fhir.r5.model.HumanName name = new org.hl7.fhir.r5.model.HumanName()
			.setFamily(this.getNameLast())
			.addGiven(this.getNameFirst())
			.addGiven(this.getNameMiddle());
		if (this.getNameType() != null) {
			name.addExtension().setUrl(V_2_NAME_TYPE).setValue(new org.hl7.fhir.r5.model.Coding(V_2_NAME_TYPE_SYSTEM, this.getNameType(), ""));
		}
		return name;
	}

	public static ModelName fromR4(org.hl7.fhir.r4.model.HumanName name) {
		return new ModelName(name);
	}

	public static ModelName fromR5(org.hl7.fhir.r5.model.HumanName name) {
		return new ModelName(name);
	}

	public String asSingleString() {
		return this.nameFirst +
			(StringUtils.isNotBlank(this.nameMiddle) ? ", " + this.nameMiddle : "") +
			" " +
			this.nameLast;
	}

	@Override
	public String toString() {
		return "ModelName{" +
				"nameLast='" + nameLast + '\'' +
				", nameFirst='" + nameFirst + '\'' +
				", nameMiddle='" + nameMiddle + '\'' +
				", nameType='" + nameType + '\'' +
				'}';
	}
}
