package org.immregistries.iis.kernal.nuva.model;

import java.util.Objects;

public class NuvaDistribution {
	private String areaId;
	private String name;
	private String startsOn;
	private String endsOn;

	public NuvaDistribution() {
	}

	public NuvaDistribution(String areaId, String name, String startsOn, String endsOn) {
		this.areaId = areaId;
		this.name = name;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
	}

	// Getters and Setters
	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStartsOn() {
		return startsOn;
	}

	public void setStartsOn(String startsOn) {
		this.startsOn = startsOn;
	}

	public String getEndsOn() {
		return endsOn;
	}

	public void setEndsOn(String endsOn) {
		this.endsOn = endsOn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaDistribution that = (NuvaDistribution) o;
		return Objects.equals(areaId, that.areaId) && Objects.equals(name, that.name) && Objects.equals(startsOn, that.startsOn) && Objects.equals(endsOn, that.endsOn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(areaId, name, startsOn, endsOn);
	}
}