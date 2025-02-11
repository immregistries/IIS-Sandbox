package org.immregistries.iis.kernal.model;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.mapping.MappingHelper;

public class ModelPhone {
	public static final String PHONE_USE_V2_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0201";
	public static final String USE_EXTENSION_URL = "use";
	private String number = "";
	private String use = "";

	private ModelPhone(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
		setNumber(contactPoint.getValue());
		org.hl7.fhir.r4.model.Extension useExtension = contactPoint.getExtensionByUrl(USE_EXTENSION_URL);
		if (useExtension != null) {
			org.hl7.fhir.r4.model.Coding coding = MappingHelper.extensionGetCoding(useExtension);
			if (coding != null && StringUtils.isNotBlank(coding.getCode())) {
				setUse(coding.getCode());
			} else {
				setUse("");
			}
		} else if (contactPoint.getUse() != null) {
			setUse(contactPoint.getUse().toCode());
		} else {
			setUse(null);
		}
	}

	private ModelPhone(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
		setNumber(contactPoint.getValue());
		org.hl7.fhir.r5.model.Extension useExtension = contactPoint.getExtensionByUrl(USE_EXTENSION_URL);
		if (useExtension != null) {
			org.hl7.fhir.r5.model.Coding coding = MappingHelper.extensionGetCoding(useExtension);
			if (coding != null && StringUtils.isNotBlank(coding.getCode())) {
				setUse(coding.getCode());
			} else {
				setUse("");
			}
		} else if (contactPoint.getUse() != null) {
			setUse(contactPoint.getUse().toCode());
		} else {
			setUse(null);
		}
	}

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


	public org.hl7.fhir.r4.model.ContactPoint toR4() {
		org.hl7.fhir.r4.model.ContactPoint contactPoint = new org.hl7.fhir.r4.model.ContactPoint();
		contactPoint.setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)
			.setValue(number);
		if (use != null) {
			switch (use) {
				case "": {
					break;
				}
				case "PRN":
				case "ORN":
				case "VHN": {
					contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.HOME);
					break;
				}
				case "WPN": {
					contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.WORK);
					break;
				}
				case "PRS": {
					contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.MOBILE);
					break;
				}
				default: {
					contactPoint.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.fromCode(use));
					break;
				}
			}
			if (use != null) {
				contactPoint.addExtension(USE_EXTENSION_URL, new org.hl7.fhir.r4.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
			}
		}
		return contactPoint;
	}

	public org.hl7.fhir.r5.model.ContactPoint toR5() {
		org.hl7.fhir.r5.model.ContactPoint contactPoint = new org.hl7.fhir.r5.model.ContactPoint();
		contactPoint.setSystem(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)
			.setValue(number);
		if (use != null) {
			switch (use) {
				case "": {
					break;
				}
				case "PRN":
				case "ORN":
				case "VHN": {
					contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.HOME);
					break;
				}
				case "WPN": {
					contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.WORK);
					break;
				}
				case "PRS": {
					contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.MOBILE);
					break;
				}
				default: {
					contactPoint.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.fromCode(use));
					break;
				}
			}
			if (use != null) {
				contactPoint.addExtension(USE_EXTENSION_URL, new org.hl7.fhir.r5.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
			}
		}
		return contactPoint;
	}

	public static ModelPhone fromR4(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
		if (!contactPoint.getSystem().equals(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)) {
			return null;
		} else {
			return new ModelPhone(contactPoint);
		}
	}

	public static ModelPhone fromR5(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
		if (!contactPoint.getSystem().equals(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)) {
			return null;
		} else {
			return new ModelPhone(contactPoint);
		}
	}

	public static ModelPhone fromFhir(org.hl7.fhir.instance.model.api.ICompositeType contactPoint) {
		if (contactPoint instanceof org.hl7.fhir.r5.model.ContactPoint) {
			return fromR5((org.hl7.fhir.r5.model.ContactPoint) contactPoint);
		} else if (contactPoint instanceof org.hl7.fhir.r4.model.ContactPoint) {
			return fromR4((org.hl7.fhir.r4.model.ContactPoint) contactPoint);
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return "PatientPhone{" +
			"number='" + number + '\'' +
			", use='" + use + '\'' +
			'}';
	}
}
