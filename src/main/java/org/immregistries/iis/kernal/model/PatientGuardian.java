package org.immregistries.iis.kernal.model;

public class PatientGuardian {
	private ModelName name = new ModelName();
	private String guardianRelationship = "";

	public String getGuardianRelationship() {
		return guardianRelationship;
	}

	public void setGuardianRelationship(String guardianRelationship) {
		this.guardianRelationship = guardianRelationship;
	}

	public ModelName getName() {
		return name;
	}

	public void setName(ModelName name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "PatientGuardian{" +
				"name=" + name +
				", guardianRelationship='" + guardianRelationship + '\'' +
				'}';
	}
}
