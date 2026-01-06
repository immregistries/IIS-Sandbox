package org.immregistries.iis.kernal.model;

import java.util.Objects;

public class ModelAddress extends AbstractDiffable<ModelAddress> {
	private String addressLine1 = "";
	private String addressLine2 = "";
	private String addressCity = "";
	private String addressState = "";
	private String addressZip = "";
	private String addressCountry = "";
	private String addressCountyParish = "";

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	public String getAddressCity() {
		return addressCity;
	}

	public void setAddressCity(String addressCity) {
		this.addressCity = addressCity;
	}

	public String getAddressState() {
		return addressState;
	}

	public void setAddressState(String addressState) {
		this.addressState = addressState;
	}

	public String getAddressZip() {
		return addressZip;
	}

	public void setAddressZip(String addressZip) {
		this.addressZip = addressZip;
	}

	public String getAddressCountry() {
		return addressCountry;
	}

	public void setAddressCountry(String addressCountry) {
		this.addressCountry = addressCountry;
	}

	public String getAddressCountyParish() {
		return addressCountyParish;
	}

	public void setAddressCountyParish(String addressCountyParish) {
		this.addressCountyParish = addressCountyParish;
	}

	@Override
	public String toString() {
		return "PatientAddress{" +
				"addressLine1='" + addressLine1 + '\'' +
				", addressLine2='" + addressLine2 + '\'' +
				", addressCity='" + addressCity + '\'' +
				", addressState='" + addressState + '\'' +
				", addressZip='" + addressZip + '\'' +
				", addressCountry='" + addressCountry + '\'' +
				", addressCountyParish='" + addressCountyParish + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ModelAddress that = (ModelAddress) o;
		return Objects.equals(addressLine1, that.addressLine1) && Objects.equals(addressLine2, that.addressLine2)
				&& Objects.equals(addressCity, that.addressCity) && Objects.equals(addressState, that.addressState)
				&& Objects.equals(addressZip, that.addressZip) && Objects.equals(addressCountry, that.addressCountry)
				&& Objects.equals(addressCountyParish, that.addressCountyParish);
	}

	@Override
	public int hashCode() {
		return Objects.hash(addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry,
				addressCountyParish);
	}
}
