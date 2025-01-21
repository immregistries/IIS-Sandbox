package org.immregistries.iis.kernal.model;

public class PatientPhone {
	private String number = "";
	private String use = "";

	private PatientPhone(org.hl7.fhir.r4.model.ContactPoint contactPoint) {
		number = contactPoint.getValue();
		use = contactPoint.getUse().toCode();
	}

	private PatientPhone(org.hl7.fhir.r5.model.ContactPoint contactPoint) {
		number = contactPoint.getValue();
		use = contactPoint.getUse().toCode();
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
			.setValue(number)
			.setUse(org.hl7.fhir.r4.model.ContactPoint.ContactPointUse.fromCode(use));
		return contactPoint;
	}

	public org.hl7.fhir.r5.model.ContactPoint toR5() {
		org.hl7.fhir.r5.model.ContactPoint contactPoint = new org.hl7.fhir.r5.model.ContactPoint();
		contactPoint.setSystem(org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem.PHONE)
			.setValue(number)
			.setUse(org.hl7.fhir.r5.model.ContactPoint.ContactPointUse.fromCode(use));
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
