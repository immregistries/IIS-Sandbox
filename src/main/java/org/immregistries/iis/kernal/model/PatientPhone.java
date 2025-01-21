package org.immregistries.iis.kernal.model;

public class PatientPhone {
	public static final String PHONE_USE_V2_SYSTEM = "http://terminology.hl7.org/ValueSet/v2-0201";
	public static final String USE_EXTENSION_URL = "use";
	private String number = "";
	private String use = "";

	private PatientPhone(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
		number = contactPoint.getValue();
		if (contactPoint.hasExtension(USE_EXTENSION_URL)) {
			use = ((org.hl7.fhir.r4.model.Coding) contactPoint.getExtensionByUrl(USE_EXTENSION_URL).getValue()).getCode();
		} else if (contactPoint.getUse() != null) {
			use = contactPoint.getUse().toCode();
		}
	}

	private PatientPhone(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
		number = contactPoint.getValue();
		if (contactPoint.hasExtension(USE_EXTENSION_URL)) {
			use = ((org.hl7.fhir.r5.model.Coding) contactPoint.getExtensionByUrl(USE_EXTENSION_URL).getValue()).getCode();
		} else if (contactPoint.getUse() != null) {
			use = contactPoint.getUse().toCode();
		}
	}

	public PatientPhone() {
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
			contactPoint.addExtension(USE_EXTENSION_URL, new org.hl7.fhir.r4.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
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
			contactPoint.addExtension(USE_EXTENSION_URL, new org.hl7.fhir.r5.model.Coding().setSystem(PHONE_USE_V2_SYSTEM).setCode(use));
		}
		return contactPoint;
	}

	public static PatientPhone fromR4(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
		if (!contactPoint.getSystem().equals(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE)) {
			return null;
		} else {
			return new PatientPhone(contactPoint);
		}
	}

	public static PatientPhone fromR5(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
		if (!contactPoint.getSystem().equals(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)) {
			return null;
		} else {
			return new PatientPhone(contactPoint);
		}
	}

	public static PatientPhone fromFhir(org.hl7.fhir.instance.model.api.ICompositeType contactPoint) {
		if (contactPoint instanceof org.hl7.fhir.r5.model.ContactPoint) {
			return fromR5((org.hl7.fhir.r5.model.ContactPoint) contactPoint);
		} else if (contactPoint instanceof org.hl7.fhir.r4.model.ContactPoint) {
			return fromR4((org.hl7.fhir.r4.model.ContactPoint) contactPoint);
		} else {
			return null;
		}
	}
}
