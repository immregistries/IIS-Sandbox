package org.immregistries.iis.kernal.nuva.model;

import java.util.List;
import java.util.Objects;

public class NuvaVaccine {
	private String id;
	private NuvaTranslatedField name;
	private Integer code;
	private String codeNuva;
	private List<NuvaCode> codes;
	private List<NuvaValence> valences;
	private List<NuvaInjectionMethod> injectionMethods;
	private NuvaTranslatedField shortDescription;
	private NuvaTranslatedField description;
	private List<String> otherNames;
	private List<NuvaDistribution> distributions;
	private Boolean generic;
	private String replacedById;
	private String createdAt;
	private String updatedAt;

	public NuvaVaccine() {
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

	public String getCodeNuva() {
		return codeNuva;
	}

	public void setCodeNuva(String codeNuva) {
		this.codeNuva = codeNuva;
	}

	public List<NuvaCode> getCodes() {
		return codes;
	}

	public void setCodes(List<NuvaCode> codes) {
		this.codes = codes;
	}

	public List<NuvaValence> getValences() {
		return valences;
	}

	public void setValences(List<NuvaValence> valences) {
		this.valences = valences;
	}

	public List<NuvaInjectionMethod> getInjectionMethods() {
		return injectionMethods;
	}

	public void setInjectionMethods(List<NuvaInjectionMethod> injectionMethods) {
		this.injectionMethods = injectionMethods;
	}

	public NuvaTranslatedField getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(NuvaTranslatedField shortDescription) {
		this.shortDescription = shortDescription;
	}

	public NuvaTranslatedField getDescription() {
		return description;
	}

	public void setDescription(NuvaTranslatedField description) {
		this.description = description;
	}

	public List<String> getOtherNames() {
		return otherNames;
	}

	public void setOtherNames(List<String> otherNames) {
		this.otherNames = otherNames;
	}

	public List<NuvaDistribution> getDistributions() {
		return distributions;
	}

	public void setDistributions(List<NuvaDistribution> distributions) {
		this.distributions = distributions;
	}

	public Boolean getGeneric() {
		return generic;
	}

	public void setGeneric(Boolean generic) {
		this.generic = generic;
	}

	public String getReplacedById() {
		return replacedById;
	}

	public void setReplacedById(String replacedById) {
		this.replacedById = replacedById;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NuvaVaccine that = (NuvaVaccine) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(code, that.code) && Objects.equals(codeNuva, that.codeNuva) && Objects.equals(codes, that.codes) && Objects.equals(valences, that.valences) && Objects.equals(injectionMethods, that.injectionMethods) && Objects.equals(shortDescription, that.shortDescription) && Objects.equals(description, that.description) && Objects.equals(otherNames, that.otherNames) && Objects.equals(distributions, that.distributions) && Objects.equals(generic, that.generic) && Objects.equals(replacedById, that.replacedById) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
	}
}