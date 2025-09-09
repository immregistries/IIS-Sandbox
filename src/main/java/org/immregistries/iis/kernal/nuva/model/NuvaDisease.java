package org.immregistries.iis.kernal.nuva.model;

import java.util.List;
import java.util.Objects;

public class NuvaDisease {
	private String id;
	private NuvaTranslatedField name;
	private Integer code;
	private List<NuvaCode> codes;
	private List<String> valenceIds;
	private Boolean screenable;
	private Boolean vaccinable;
	private String updatedAt;
	private String createdAt;

	public NuvaDisease() {
	}

	public NuvaDisease(String id, NuvaTranslatedField name, Integer code, List<NuvaCode> codes, List<String> valenceIds, Boolean screenable, Boolean vaccinable, String updatedAt, String createdAt) {
		this.id = id;
		this.name = name;
		this.code = code;
		this.codes = codes;
		this.valenceIds = valenceIds;
		this.screenable = screenable;
		this.vaccinable = vaccinable;
		this.updatedAt = updatedAt;
		this.createdAt = createdAt;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public NuvaTranslatedField getName() {
		return name;
	}

	public void setName(NuvaTranslatedField name) {
		this.name = name;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public List<NuvaCode> getCodes() {
		return codes;
	}

	public void setCodes(List<NuvaCode> codes) {
		this.codes = codes;
	}

	public List<String> getValenceIds() {
		return valenceIds;
	}

	public void setValenceIds(List<String> valenceIds) {
		this.valenceIds = valenceIds;
	}

	public Boolean getScreenable() {
		return screenable;
	}

	public void setScreenable(Boolean screenable) {
		this.screenable = screenable;
	}

	public Boolean getVaccinable() {
		return vaccinable;
	}

	public void setVaccinable(Boolean vaccinable) {
		this.vaccinable = vaccinable;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaDisease that = (NuvaDisease) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(code, that.code) && Objects.equals(codes, that.codes) && Objects.equals(valenceIds, that.valenceIds) && Objects.equals(screenable, that.screenable) && Objects.equals(vaccinable, that.vaccinable) && Objects.equals(updatedAt, that.updatedAt) && Objects.equals(createdAt, that.createdAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, code, codes, valenceIds, screenable, vaccinable, updatedAt, createdAt);
	}
}