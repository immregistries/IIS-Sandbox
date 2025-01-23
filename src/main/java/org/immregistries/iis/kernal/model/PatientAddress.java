package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.StringUtils;

public class PatientAddress {
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

	public org.hl7.fhir.r4.model.Address toR4() {
		return new org.hl7.fhir.r4.model.Address().addLine(getAddressLine1())
			.addLine(getAddressLine2())
			.setCity(getAddressCity())
			.setCountry(getAddressCountry())
			.setState(getAddressState())
			.setDistrict(getAddressCountyParish())
			.setPostalCode(getAddressZip());
	}

	public org.hl7.fhir.r5.model.Address toR5() {
		return new org.hl7.fhir.r5.model.Address().addLine(getAddressLine1())
			.addLine(getAddressLine2())
			.setCity(getAddressCity())
			.setCountry(getAddressCountry())
			.setState(getAddressState())
			.setDistrict(getAddressCountyParish())
			.setPostalCode(getAddressZip());
	}

	public static PatientAddress fromR4(org.hl7.fhir.r4.model.Address address) {
		PatientAddress patientAddress = new PatientAddress();
		if (!address.getLine().isEmpty()) {
			patientAddress.setAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			patientAddress.setAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		patientAddress.setAddressCity(StringUtils.defaultString(address.getCity()));
		patientAddress.setAddressState(StringUtils.defaultString(address.getState()));
		patientAddress.setAddressZip(StringUtils.defaultString(address.getPostalCode()));
		patientAddress.setAddressCountry(StringUtils.defaultString(address.getCountry()));
		patientAddress.setAddressCountyParish(StringUtils.defaultString(address.getDistrict()));
		return patientAddress;
	}

	public static PatientAddress fromR5(org.hl7.fhir.r5.model.Address address) {
		PatientAddress patientAddress = new PatientAddress();
		if (!address.getLine().isEmpty()) {
			patientAddress.setAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			patientAddress.setAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		patientAddress.setAddressCity(StringUtils.defaultString(address.getCity()));
		patientAddress.setAddressState(StringUtils.defaultString(address.getState()));
		patientAddress.setAddressZip(StringUtils.defaultString(address.getPostalCode()));
		patientAddress.setAddressCountry(StringUtils.defaultString(address.getCountry()));
		patientAddress.setAddressCountyParish(StringUtils.defaultString(address.getDistrict()));
		return patientAddress;
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
}
