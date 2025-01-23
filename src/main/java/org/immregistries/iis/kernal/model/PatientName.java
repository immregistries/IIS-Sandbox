package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.mapping.MappingHelper;

import static org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper.V_2_NAME_TYPE;
import static org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper.V_2_NAME_TYPE_SYSTEM;

public class PatientName {
	private String nameLast = "";
	private String nameFirst = "";
	private String nameMiddle = "";
	private String nameType = "";

	public PatientName(String nameLast, String nameFirst, String nameMiddle, String nameType) {
		this.nameLast = nameLast;
		this.nameFirst = nameFirst;
		this.nameMiddle = nameMiddle;
		this.nameType = nameType;
	}

	private PatientName(org.hl7.fhir.r4.model.HumanName name) {
		this.setNameLast(name.getFamily());
		if (!name.getGiven().isEmpty()) {
			this.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			this.setNameMiddle(name.getGiven().get(1).getValueNotNull());
		}
		org.hl7.fhir.r4.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
		if (nameType != null) {
			this.setNameType(MappingHelper.extensionGetCoding(nameType).getCode());
		}
	}

	private PatientName(org.hl7.fhir.r5.model.HumanName name) {
		this.setNameLast(name.getFamily());
		if (!name.getGiven().isEmpty()) {
			this.setNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			this.setNameMiddle(name.getGiven().get(1).getValueNotNull());
		}
		org.hl7.fhir.r5.model.Extension nameType = name.getExtensionByUrl(V_2_NAME_TYPE);
		if (nameType != null) {
			this.setNameType(MappingHelper.extensionGetCoding(nameType).getCode());
		}
	}

	public PatientName() {
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
		if (StringUtils.isNotBlank(this.getNameType())) {
			name.addExtension().setUrl(V_2_NAME_TYPE).setValue(new org.hl7.fhir.r4.model.Coding(V_2_NAME_TYPE_SYSTEM, this.getNameType(), ""));
		}
		return name;
	}

	public org.hl7.fhir.r5.model.HumanName toR5() {
		org.hl7.fhir.r5.model.HumanName name = new org.hl7.fhir.r5.model.HumanName()
			.setFamily(this.getNameLast())
			.addGiven(this.getNameFirst())
			.addGiven(this.getNameMiddle());
		if (StringUtils.isNotBlank(this.getNameType())) {
			name.addExtension().setUrl(V_2_NAME_TYPE).setValue(new org.hl7.fhir.r5.model.Coding(V_2_NAME_TYPE_SYSTEM, this.getNameType(), ""));
		}
		return name;
	}

	public static PatientName fromR4(org.hl7.fhir.r4.model.HumanName name) {
		return new PatientName(name);
	}

	public static PatientName fromR5(org.hl7.fhir.r5.model.HumanName name) {
		return new PatientName(name);
	}

	@Override
	public String toString() {
		return "PatientName{" +
				"nameLast='" + nameLast + '\'' +
				", nameFirst='" + nameFirst + '\'' +
				", nameMiddle='" + nameMiddle + '\'' +
				", nameType='" + nameType + '\'' +
				'}';
	}
}
