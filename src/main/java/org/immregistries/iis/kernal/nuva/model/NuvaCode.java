package org.immregistries.iis.kernal.nuva.model;

import java.util.Objects;

public class NuvaCode {
	private String value;
	private NuvaNomenclature nomenclature;

	public NuvaCode() {
	}

	public NuvaCode(String value, NuvaNomenclature nomenclature) {
		this.value = value;
		this.nomenclature = nomenclature;
	}

	// Getters and Setters
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public NuvaNomenclature getNomenclature() {
		return nomenclature;
	}

	public void setNomenclature(NuvaNomenclature nomenclature) {
		this.nomenclature = nomenclature;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaCode nuvaCode = (NuvaCode) o;
		return Objects.equals(value, nuvaCode.value) && nomenclature == nuvaCode.nomenclature;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, nomenclature);
	}
}