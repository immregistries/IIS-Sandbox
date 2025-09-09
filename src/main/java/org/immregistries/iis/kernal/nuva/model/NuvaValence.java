package org.immregistries.iis.kernal.nuva.model;

import java.util.List;
import java.util.Objects;

public class NuvaValence {
	private String id;
	private Integer code;
	private NuvaTranslatedField name;
	private String antigeneType;
	private String abbreviation;
	private NuvaTranslatedField translatedAbbreviation;
	private List<String> diseaseIds;
	private List<String> vaccineIds;
	private String createdAt;
	private String updatedAt;
	private String parentId;

	public NuvaValence() {
	}

	public NuvaValence(String id, Integer code, NuvaTranslatedField name, String antigeneType, String abbreviation, NuvaTranslatedField translatedAbbreviation, List<String> diseaseIds, List<String> vaccineIds, String createdAt, String updatedAt, String parentId) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.antigeneType = antigeneType;
		this.abbreviation = abbreviation;
		this.translatedAbbreviation = translatedAbbreviation;
		this.diseaseIds = diseaseIds;
		this.vaccineIds = vaccineIds;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.parentId = parentId;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public NuvaTranslatedField getName() {
		return name;
	}

	public void setName(NuvaTranslatedField name) {
		this.name = name;
	}

	public String getAntigeneType() {
		return antigeneType;
	}

	public void setAntigeneType(String antigeneType) {
		this.antigeneType = antigeneType;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public NuvaTranslatedField getTranslatedAbbreviation() {
		return translatedAbbreviation;
	}

	public void setTranslatedAbbreviation(NuvaTranslatedField translatedAbbreviation) {
		this.translatedAbbreviation = translatedAbbreviation;
	}

	public List<String> getDiseaseIds() {
		return diseaseIds;
	}

	public void setDiseaseIds(List<String> diseaseIds) {
		this.diseaseIds = diseaseIds;
	}

	public List<String> getVaccineIds() {
		return vaccineIds;
	}

	public void setVaccineIds(List<String> vaccineIds) {
		this.vaccineIds = vaccineIds;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaValence that = (NuvaValence) o;
		return Objects.equals(id, that.id) && Objects.equals(code, that.code) && Objects.equals(name, that.name) && Objects.equals(antigeneType, that.antigeneType) && Objects.equals(abbreviation, that.abbreviation) && Objects.equals(translatedAbbreviation, that.translatedAbbreviation) && Objects.equals(diseaseIds, that.diseaseIds) && Objects.equals(vaccineIds, that.vaccineIds) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt) && Objects.equals(parentId, that.parentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, code, name, antigeneType, abbreviation, translatedAbbreviation, diseaseIds, vaccineIds, createdAt, updatedAt, parentId);
	}
}