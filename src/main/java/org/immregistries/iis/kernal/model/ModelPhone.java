package org.immregistries.iis.kernal.model;

import java.util.Objects;

public class ModelPhone extends AbstractDiffable<ModelPhone> {
	private String number = "";
	private String use = "";

	public ModelPhone() {
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getUse() {
		return use;
	}

	public void setUse(String use) {
		this.use = use;
	}

	@Override
	public String toString() {
		return "ModelPhone{" +
				"number='" + number + '\'' +
				", use='" + use + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ModelPhone that = (ModelPhone) o;
		return Objects.equals(number, that.number) && Objects.equals(use, that.use);
	}

	@Override
	public int hashCode() {
		return Objects.hash(number, use);
	}
}
