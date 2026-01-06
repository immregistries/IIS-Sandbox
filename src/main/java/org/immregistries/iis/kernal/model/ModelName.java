package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ModelName modelName = (ModelName) o;
		return Objects.equals(nameLast, modelName.nameLast) && Objects.equals(nameFirst, modelName.nameFirst)
				&& Objects.equals(nameMiddle, modelName.nameMiddle) && Objects.equals(nameType, modelName.nameType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nameLast, nameFirst, nameMiddle, nameType);
	}
}
