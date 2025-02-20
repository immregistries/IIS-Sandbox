package org.immregistries.iis.kernal.model;

import java.util.Objects;

public class PatientGuardian extends AbstractDiffable<PatientGuardian> {
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

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		PatientGuardian that = (PatientGuardian) o;
		return Objects.equals(name, that.name) && Objects.equals(guardianRelationship, that.guardianRelationship);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, guardianRelationship);
	}
}
