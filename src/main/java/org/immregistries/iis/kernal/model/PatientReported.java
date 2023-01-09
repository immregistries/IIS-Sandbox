package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientReported implements Serializable {

  private static final long serialVersionUID = 1L;

	private String id = "";
	private OrgMaster orgReported = null;
	private String patientReportedExternalLink = "";
	private PatientMaster patient = null;
	private Date reportedDate = null;
	private Date updatedDate = null;

	private String patientReportedAuthority = "";
	private String patientReportedType = "";
	private String nameLast = "";
	private String nameFirst = "";
	private String nameMiddle = "";
	private String motherMaidenName = "";
	private Date birthDate = null;
	private String sex = "";
	private String race = "";
	private String race2 = "";
	private String race3 = "";
	private String race4 = "";
	private String race5 = "";
	private String race6 = "";
	private String addressLine1 = "";
	private String addressLine2 = "";
	private String addressCity = "";
	private String addressState = "";
	private String addressZip = "";
	private String addressCountry = "";
	private String addressCountyParish = "";
	private String phone = "";
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
	private String guardianLast = "";
	private String guardianFirst = "";
	private String guardianMiddle = "";
	private String guardianRelationship = "";


	private String managingOrganizationId;

	public String getManagingOrganizationId() {
		return managingOrganizationId;
	}

	public void setManagingOrganizationId(String managingOrganizationId) {
		this.managingOrganizationId = managingOrganizationId;
	}

	public String getPatientReportedAuthority() {
    return patientReportedAuthority;
  }

  public void setPatientReportedAuthority(String patientReportedAuthority) {
    this.patientReportedAuthority = patientReportedAuthority;
  }

	public String getPatientReportedType() {
		return patientReportedType;
	}

	public void setPatientReportedType(String patientReportedType) {
		this.patientReportedType = patientReportedType;
	}

	public String getNameLast() {
		return nameLast;
	}

	public void setNameLast(String nameLast) {
		this.nameLast = nameLast;
	}

	public String getNameFirst() {
		return nameFirst;
	}

	public void setNameFirst(String nameFirst) {
		this.nameFirst = nameFirst;
	}

	public String getNameMiddle() {
		return nameMiddle;
	}

	public void setNameMiddle(String nameMiddle) {
		this.nameMiddle = nameMiddle;
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

	public String getRace() {
		return race;
	}

	public void setRace(String race) {
		this.race = race;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	public String getAddressCity() {
		return addressCity;
	}

	public void setAddressCity(String addressCity) {
		this.addressCity = addressCity;
	}

	public String getAddressState() {
		return addressState;
	}

	public void setAddressState(String addressState) {
		this.addressState = addressState;
	}

	public String getAddressZip() {
		return addressZip;
	}

	public void setAddressZip(String addressZip) {
		this.addressZip = addressZip;
	}

	public String getAddressCountry() {
		return addressCountry;
	}

	public void setAddressCountry(String addressCountry) {
		this.addressCountry = addressCountry;
	}

	public String getAddressCountyParish() {
		return addressCountyParish;
	}

	public void setAddressCountyParish(String addressCountyParish) {
		this.addressCountyParish = addressCountyParish;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
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

  public String getGuardianLast() {
    return guardianLast;
  }

  public void setGuardianLast(String guardianLast) {
    this.guardianLast = guardianLast;
  }

  public String getGuardianFirst() {
    return guardianFirst;
  }

  public void setGuardianFirst(String guardianFirst) {
    this.guardianFirst = guardianFirst;
  }

  public String getGuardianMiddle() {
    return guardianMiddle;
  }

  public void setGuardianMiddle(String guardianMiddle) {
    this.guardianMiddle = guardianMiddle;
  }

  public String getGuardianRelationship() {
    return guardianRelationship;
  }

  public void setGuardianRelationship(String guardianRelationship) {
    this.guardianRelationship = guardianRelationship;
  }

	public OrgMaster getOrgReported() {
		return orgReported;
	}

	public void setOrgReported(OrgMaster reportedOrg) {
		this.orgReported = reportedOrg;
	}

	public String getId() {
		return id;
	}

	public void setId(String reportedPatientId) {
		this.id = reportedPatientId;
	}

	public String getPatientReportedExternalLink() {
		return patientReportedExternalLink;
	}
//  public String getPatientReportedExternalLink() {
//    return patientReportedId;
//  }

	public void setPatientReportedExternalLink(String reportedMrn) {
    this.patientReportedExternalLink = reportedMrn;
  }
//  public void setPatientReportedExternalLink(String reportedMrn) {
//    this.patientReportedId = reportedMrn;
//  }

  public PatientMaster getPatient() {
    return patient;
  }

  public void setPatient(PatientMaster patient) {
    this.patient = patient;
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

	public String getRace2() {
		return race2;
	}

	public void setRace2(String race2) {
		this.race2 = race2;
	}

	public String getRace3() {
		return race3;
	}

	public void setRace3(String race3) {
		this.race3 = race3;
	}

	public String getRace4() {
		return race4;
	}

	public void setRace4(String race4) {
		this.race4 = race4;
	}

	public String getRace5() {
		return race5;
	}

	public void setRace5(String race5) {
		this.race5 = race5;
	}

	public String getRace6() {
		return race6;
	}

	public void setRace6(String race6) {
		this.race6 = race6;
	}

//  @Override
//  public String toString() {
//    return "PatientReported{" + "patientReportedId=" + id + ", orgReported="
//        + orgReported
//		 + ", patientReportedExternalLink='" + patientReportedExternalLink + '\''
//        + ", patient=" + patient + ", reportedDate=" + reportedDate + ", updatedDate=" + updatedDate
//        + ", patientReportedAuthority='" + patientReportedAuthority + '\''
//        + ", patientReportedType='" + patientReportedType + '\'' + ", patientNameLast='"
//        + nameLast + '\'' + ", patientNameFirst='" + nameFirst + '\''
//        + ", patientNameMiddle='" + nameMiddle + '\'' + ", patientMotherMaiden='"
//        + motherMaidenName + '\'' + ", patientBirthDate=" + birthDate + ", patientSex='"
//        + sex + '\'' + ", patientRace='" + race + '\'' + ", patientRace2='"
//        + race2 + '\'' + ", patientRace3='" + race3 + '\'' + ", patientRace4='"
//        + race4 + '\'' + ", patientRace5='" + race5 + '\'' + ", patientRace6='"
//        + race6 + '\'' + ", patientAddressLine1='" + addressLine1 + '\''
//        + ", patientAddressLine2='" + addressLine2 + '\'' + ", patientAddressCity='"
//        + addressCity + '\'' + ", patientAddressState='" + addressState + '\''
//        + ", patientAddressZip='" + addressZip + '\'' + ", patientAddressCountry='"
//        + addressCountry + '\'' + ", patientAddressCountyParish='"
//        + addressCountyParish + '\'' + ", patientPhone='" + phone + '\''
//        + ", patientEmail='" + email + '\'' + ", patientEthnicity='" + ethnicity
//        + '\'' + ", patientBirthFlag='" + birthFlag + '\'' + ", patientBirthOrder='"
//        + birthOrder + '\'' + ", patientDeathFlag='" + deathFlag + '\''
//        + ", patientDeathDate=" + deathDate + ", publicityIndicator='" + publicityIndicator
//        + '\'' + ", publicityIndicatorDate=" + publicityIndicatorDate + ", protectionIndicator='"
//        + protectionIndicator + '\'' + ", protectionIndicatorDate=" + protectionIndicatorDate
//        + ", registryStatusIndicator='" + registryStatusIndicator + '\''
//        + ", registryStatusIndicatorDate=" + registryStatusIndicatorDate + ", guardianLast='"
//        + guardianLast + '\'' + ", guardianFirst='" + guardianFirst + '\'' + ", guardianMiddle='"
//        + guardianMiddle + '\'' + ", guardianRelationship='" + guardianRelationship + '\'' + '}';
//  }
}
