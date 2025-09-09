package org.immregistries.iis.kernal.nuva.model;

import java.util.Objects;

public class NuvaVersion {
	private Integer number;
	private Integer major;
	private Integer minor;
	private Integer patch;

	public NuvaVersion() {
	}

	public NuvaVersion(Integer number, Integer major, Integer minor, Integer patch) {
		this.number = number;
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	// Getters and Setters
	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Integer getMajor() {
		return major;
	}

	public void setMajor(Integer major) {
		this.major = major;
	}

	public Integer getMinor() {
		return minor;
	}

	public void setMinor(Integer minor) {
		this.minor = minor;
	}

	public Integer getPatch() {
		return patch;
	}

	public void setPatch(Integer patch) {
		this.patch = patch;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaVersion that = (NuvaVersion) o;
		return Objects.equals(number, that.number) && Objects.equals(major, that.major) && Objects.equals(minor, that.minor) && Objects.equals(patch, that.patch);
	}

	@Override
	public int hashCode() {
		return Objects.hash(number, major, minor, patch);
	}
}