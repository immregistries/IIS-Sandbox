package org.immregistries.iis.kernal.logic.shlink.evc;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * A Java class that represents the EvC (European vaccination certificate) payload structure.
 * This class is designed to be serialized and deserialized using the Jackson library.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvCPayload implements Serializable {
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static final String DOB = "dob";
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty("ver")
	private String version;

	@JsonProperty("nam")
	private Name name;

//	@JsonProperty(DOB)
	/*
	 * See getter and setter for logic with formatting
	 */
	@JsonIgnore()
	private Date dateOfBirth;

	@JsonProperty("pid")
	private PersonIdentifier personIdentifier;

	@JsonProperty("v")
	private List<VaccinationRecord> vaccinationRecords;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	@JsonGetter(DOB)
	public String getJsonDateOfBirth() {
		return simpleDateFormat.format(getDateOfBirth());
	}

	@JsonSetter(DOB)
	public void setJsonDateOfBirth(String string) throws ParseException {
		setDateOfBirth(simpleDateFormat.parse(string));
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public PersonIdentifier getPersonIdentifier() {
		return personIdentifier;
	}

	public void setPersonIdentifier(PersonIdentifier personIdentifier) {
		this.personIdentifier = personIdentifier;
	}

	public List<VaccinationRecord> getVaccinationRecords() {
		return vaccinationRecords;
	}

	public void setVaccinationRecords(List<VaccinationRecord> vaccinationRecords) {
		this.vaccinationRecords = vaccinationRecords;
	}

	@Override
	public String toString() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}

	/**
	 * Nested class representing the "nam" structure.
	 */
	public static class Name implements Serializable {
		@JsonProperty("fnt")
		private String familyName;

		@JsonProperty("gnt")
		private String givenName;

		public String getFamilyName() {
			return familyName;
		}

		public void setFamilyName(String familyName) {
			this.familyName = familyName;
		}

		public String getGivenName() {
			return givenName;
		}

		public void setGivenName(String givenName) {
			this.givenName = givenName;
		}

		@Override
		public String toString() {
			try {
				return OBJECT_MAPPER.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}
	}

	/**
	 * Nested class representing the optional "pid" structure.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PersonIdentifier implements Serializable {
		@JsonProperty("oid")
		private String objectIdentifier;

		@JsonProperty("id")
		private String id;

		public String getObjectIdentifier() {
			return objectIdentifier;
		}

		public void setObjectIdentifier(String objectIdentifier) {
			this.objectIdentifier = objectIdentifier;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			try {
				return OBJECT_MAPPER.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}
	}

	/**
	 * Nested class representing the vaccination record within the "v" array.
	 */
	public static class VaccinationRecord implements Serializable {
		@JsonProperty("reg")
		private String registryCode;

		@JsonProperty("rep")
		private int repositoryIndex;

		@JsonProperty("i")
		private int reference;

		@JsonProperty("a")
		private int ageInDays;

		@JsonProperty("mp")
		private int nuvaCode;

		public String getRegistryCode() {
			return registryCode;
		}

		public void setRegistryCode(String registryCode) {
			this.registryCode = registryCode;
		}

		public int getRepositoryIndex() {
			return repositoryIndex;
		}

		public void setRepositoryIndex(int repositoryIndex) {
			this.repositoryIndex = repositoryIndex;
		}

		public int getReference() {
			return reference;
		}

		public void setReference(int reference) {
			this.reference = reference;
		}

		public int getAgeInDays() {
			return ageInDays;
		}

		public void setAgeInDays(int ageInDays) {
			this.ageInDays = ageInDays;
		}

		public int getNuvaCode() {
			return nuvaCode;
		}

		public void setNuvaCode(int nuvaCode) {
			this.nuvaCode = nuvaCode;
		}

		@Override
		public String toString() {
			try {
				return OBJECT_MAPPER.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}
	}
}
