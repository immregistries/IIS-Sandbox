package org.immregistries.iis.kernal.model;

public class PatientGuardian {
	private PatientName name = new PatientName();
	private String guardianRelationship = "";

	public String getGuardianRelationship() {
		return guardianRelationship;
	}

	public void setGuardianRelationship(String guardianRelationship) {
		this.guardianRelationship = guardianRelationship;
	}

	public PatientName getName() {
		return name;
	}

	public void setName(PatientName name) {
		this.name = name;
	}
}
