package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Internal Standard agnostic representation of a patient's information, this class should only be used when dealing with Golden Record except for Display,
 * When dealing with a report use PatientReported
 */
public class PatientMaster extends AbstractMappedObject implements Serializable {

	private String patientId = "";
	private Tenant tenant = null;
	private List<BusinessIdentifier> businessIdentifiers = new ArrayList<>(2);
	private Date reportedDate = null;
	private Date updatedDate = null;

	private List<ModelName> modelNames = new ArrayList<ModelName>(1);
	private String motherMaidenName = "";
	private Date birthDate = null;
	private String sex = "";
	private List<String> races = new ArrayList<>(1);
	private List<ModelAddress> addresses = new ArrayList<>(1);
	private List<ModelPhone> phones = null;
	private String email = "";
	private String ethnicity = "";
	private String birthFlag = "";
	private String birthOrder = "";
	private String deathFlag = "";
	private Date deathDate = null;
	private String publicityIndicator = "";
	private Date publicityIndicatorDate = null;
	private String protectionIndicator = "";
	private Date protectionIndicatorDate = null;
	private String registryStatusIndicator = "";
	private Date registryStatusIndicatorDate = null;

	private List<PatientGuardian> patientGuardians = new ArrayList<>(1);

	private String managingOrganizationId;

	public String getManagingOrganizationId() {
		return managingOrganizationId;
	}

	public void setManagingOrganizationId(String managingOrganizationId) {
		this.managingOrganizationId = managingOrganizationId;
	}

	public String getMotherMaidenName() {
		return motherMaidenName;
	}

	public void setMotherMaidenName(String motherMaidenName) {
		this.motherMaidenName = motherMaidenName;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEthnicity() {
		return ethnicity;
	}

	public void setEthnicity(String ethnicity) {
		this.ethnicity = ethnicity;
	}

	public String getBirthFlag() {
		return birthFlag;
	}

	public void setBirthFlag(String birthFlag) {
		this.birthFlag = birthFlag;
	}

	public String getBirthOrder() {
		return birthOrder;
	}

	public void setBirthOrder(String birthOrder) {
		this.birthOrder = birthOrder;
	}

	public String getDeathFlag() {
		return deathFlag;
	}

	public void setDeathFlag(String deathFlag) {
		this.deathFlag = deathFlag;
	}

	public Date getDeathDate() {
		return deathDate;
	}

	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public String getPublicityIndicator() {
		return publicityIndicator;
	}

	public void setPublicityIndicator(String publicityIndicator) {
		this.publicityIndicator = publicityIndicator;
	}

	public Date getPublicityIndicatorDate() {
		return publicityIndicatorDate;
	}

	public void setPublicityIndicatorDate(Date publicityIndicatorDate) {
		this.publicityIndicatorDate = publicityIndicatorDate;
	}

	public String getProtectionIndicator() {
		return protectionIndicator;
	}

	public void setProtectionIndicator(String protectionIndicator) {
		this.protectionIndicator = protectionIndicator;
	}

	public Date getProtectionIndicatorDate() {
		return protectionIndicatorDate;
	}

	public void setProtectionIndicatorDate(Date protectionIndicatorDate) {
		this.protectionIndicatorDate = protectionIndicatorDate;
	}

	public String getRegistryStatusIndicator() {
		return registryStatusIndicator;
	}

	public void setRegistryStatusIndicator(String registryStatusIndicator) {
		this.registryStatusIndicator = registryStatusIndicator;
	}

	public Date getRegistryStatusIndicatorDate() {
		return registryStatusIndicatorDate;
	}

	public void setRegistryStatusIndicatorDate(Date registryStatusIndicator_date) {
		this.registryStatusIndicatorDate = registryStatusIndicator_date;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String reportedPatientId) {
		this.patientId = reportedPatientId;
	}

	public Date getReportedDate() {
		return reportedDate;
	}

	public void setReportedDate(Date reportedDate) {
		this.reportedDate = reportedDate;
	}

	public Date getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	public List<ModelName> getPatientNames() {
		return modelNames;
	}

	public void setPatientNames(List<ModelName> modelNames) {
		this.modelNames = modelNames;
	}

	public void addPatientName(ModelName modelName) {
		this.modelNames.add(modelName);
	}

	public ModelName getPatientNameFirst() {
		if (modelNames.isEmpty()) {
			return null;
		}
		return modelNames.get(0);
	}

	public ModelName getLegalName() {
		return modelNames.stream().filter(patientName -> "L".equals(patientName.getNameType())).findFirst().orElse(null);
	}

	public ModelName getLegalNameOrFirst() {
		return modelNames.stream().filter(patientName -> "L".equals(patientName.getNameType())).findFirst().orElse(this.getPatientNameFirst());
	}

	public String getNameLast() {
		return this.getLegalNameOrFirst().getNameLast();
	}

	public String getNameFirst() {
		return this.getLegalNameOrFirst().getNameFirst();
	}

	public String getNameMiddle() {
		return this.getLegalNameOrFirst().getNameMiddle();
	}

	public List<PatientGuardian> getPatientGuardians() {
		return patientGuardians;
	}

	public void setPatientGuardians(List<PatientGuardian> patientGuardians) {
		this.patientGuardians = patientGuardians;
	}

	public void addPatientGuardian(PatientGuardian patientGuardian) {
		if (this.patientGuardians == null) {
			this.patientGuardians = new ArrayList<>(1);
		}
		this.patientGuardians.add(patientGuardian);
	}

	public List<String> getRaces() {
		return races;
	}

	public void setRaces(List<String> races) {
		this.races = races;
	}

	public void addRace(String race) {
		if (this.races == null) {
			this.races = new ArrayList<>(1);
		}
		this.races.add(race);
	}

	public String getFirstRace() {
		if (races.isEmpty()) {
			return null;
		}
		return this.races.get(0);
	}

	public List<ModelPhone> getPhones() {
		return phones;
	}

	public void setPhones(List<ModelPhone> phones) {
		this.phones = phones;
	}

	public void addPhone(ModelPhone phone) {
		if (this.phones == null) {
			this.phones = new ArrayList<>(1);
		}
		this.phones.add(phone);
	}

	public ModelPhone getFirstPhone() {
		if (phones.isEmpty()) {
			return null;
		}
		return this.phones.get(0);
	}

	public List<ModelAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<ModelAddress> addresses) {
		this.addresses = addresses;
	}

	public void addAddress(ModelAddress phone) {
		if (this.addresses == null) {
			this.addresses = new ArrayList<>(1);
		}
		this.addresses.add(phone);
	}

	public ModelAddress getFirstAddress() {
		if (addresses.isEmpty()) {
			return null;
		}
		return this.addresses.get(0);
	}

	public List<BusinessIdentifier> getBusinessIdentifiers() {
		return businessIdentifiers;
	}

	public void setBusinessIdentifiers(List<BusinessIdentifier> businessIdentifiers) {
		this.businessIdentifiers = businessIdentifiers;
	}

	public void addBusinessIdentifier(BusinessIdentifier businessIdentifier) {
		if (this.businessIdentifiers == null) {
			this.businessIdentifiers = new ArrayList<>(3);
		}
		this.businessIdentifiers.add(businessIdentifier);
	}

	public BusinessIdentifier getFirstBusinessIdentifier() {
		if (businessIdentifiers.isEmpty()) {
			return null;
		}
		return this.businessIdentifiers.get(0);
	}

	public BusinessIdentifier getMainBusinessIdentifier() {
		BusinessIdentifier identifier = null;
		if (businessIdentifiers.isEmpty()) {
			return new BusinessIdentifier();
		}
		identifier = this.businessIdentifiers.stream()
			.filter(businessIdentifier -> BusinessIdentifier.MRN_TYPE_VALUE.equals(businessIdentifier.getType()))
				.findFirst()
				.orElse(null);
		if (identifier == null) {
			identifier = this.businessIdentifiers.stream()
				.filter(businessIdentifier -> "PT".equals(businessIdentifier.getType()))
					.findFirst()
					.orElse(null);
		}
		if (identifier == null) {
			identifier = this.businessIdentifiers.stream()
				.filter(businessIdentifier -> "PI".equals(businessIdentifier.getType()))
					.findFirst()
					.orElse(null);
		}
		if (identifier == null) {
			identifier = new BusinessIdentifier();
		}
		return identifier;
	}

	@Override
	public String toString() {
		return "PatientMaster{" +
				"patientId='" + patientId + '\'' +
			", tenant=" + tenant +
			", businessIdentifiers=" + businessIdentifiers +
			", reportedDate=" + reportedDate +
				", updatedDate=" + updatedDate +
			", patientNames=" + modelNames +
				", motherMaidenName='" + motherMaidenName + '\'' +
				", birthDate=" + birthDate +
				", sex='" + sex + '\'' +
				", races=" + races +
				", addresses=" + addresses +
				", phones=" + phones +
				", email='" + email + '\'' +
				", ethnicity='" + ethnicity + '\'' +
				", birthFlag='" + birthFlag + '\'' +
				", birthOrder='" + birthOrder + '\'' +
				", deathFlag='" + deathFlag + '\'' +
				", deathDate=" + deathDate +
				", publicityIndicator='" + publicityIndicator + '\'' +
				", publicityIndicatorDate=" + publicityIndicatorDate +
				", protectionIndicator='" + protectionIndicator + '\'' +
				", protectionIndicatorDate=" + protectionIndicatorDate +
				", registryStatusIndicator='" + registryStatusIndicator + '\'' +
				", registryStatusIndicatorDate=" + registryStatusIndicatorDate +
				", patientGuardians=" + patientGuardians +
			", managingOrganizationId='" + managingOrganizationId + '\'' +
				'}';
	}
}
