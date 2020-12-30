package org.immregistries.iis.kernal.model;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientReported implements Serializable {
  private int patientReportedId = 0;
  private OrgMaster orgReported = null;
  private String patientReportedExternalLink = "";
  private PatientMaster patient = null;
  private Date reportedDate = null;
  private Date updatedDate = null;

  private String patientReportedAuthority = "";
  private String patientReportedType = "";
  private String patientNameLast = "";
  private String patientNameFirst = "";
  private String patientNameMiddle = "";
  private String patientMotherMaiden = "";
  private Date patientBirthDate = null;
  private String patientSex = "";
  private String patientRace = "";
  private String patientRace2 = "";
  private String patientRace3 = "";
  private String patientRace4 = "";
  private String patientRace5 = "";
  private String patientRace6 = "";
  private String patientAddressLine1 = "";
  private String patientAddressLine2 = "";
  private String patientAddressCity = "";
  private String patientAddressState = "";
  private String patientAddressZip = "";
  private String patientAddressCountry = "";
  private String patientAddressCountyParish = "";
  private String patientPhone = "";
  private String patientEmail = "";
  private String patientEthnicity = "";
  private String patientBirthFlag = "";
  private String patientBirthOrder = "";
  private String patientDeathFlag = "";
  private Date patientDeathDate = null;
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

  public String getPatientNameLast() {
    return patientNameLast;
  }

  public void setPatientNameLast(String patientNameLast) {
    this.patientNameLast = patientNameLast;
  }

  public String getPatientNameFirst() {
    return patientNameFirst;
  }

  public void setPatientNameFirst(String patientNameFirst) {
    this.patientNameFirst = patientNameFirst;
  }

  public String getPatientNameMiddle() {
    return patientNameMiddle;
  }

  public void setPatientNameMiddle(String patientNameMiddle) {
    this.patientNameMiddle = patientNameMiddle;
  }

  public String getPatientMotherMaiden() {
    return patientMotherMaiden;
  }

  public void setPatientMotherMaiden(String patientMotherMaiden) {
    this.patientMotherMaiden = patientMotherMaiden;
  }

  public Date getPatientBirthDate() {
    return patientBirthDate;
  }

  public void setPatientBirthDate(Date patientBirthDate) {
    this.patientBirthDate = patientBirthDate;
  }

  public String getPatientSex() {
    return patientSex;
  }

  public void setPatientSex(String patientSex) {
    this.patientSex = patientSex;
  }

  public String getPatientRace() {
    return patientRace;
  }

  public void setPatientRace(String patientRace) {
    this.patientRace = patientRace;
  }

  public String getPatientAddressLine1() {
    return patientAddressLine1;
  }

  public void setPatientAddressLine1(String patientAddressLine1) {
    this.patientAddressLine1 = patientAddressLine1;
  }

  public String getPatientAddressLine2() {
    return patientAddressLine2;
  }

  public void setPatientAddressLine2(String patientAddressLine2) {
    this.patientAddressLine2 = patientAddressLine2;
  }

  public String getPatientAddressCity() {
    return patientAddressCity;
  }

  public void setPatientAddressCity(String patientAddressCity) {
    this.patientAddressCity = patientAddressCity;
  }

  public String getPatientAddressState() {
    return patientAddressState;
  }

  public void setPatientAddressState(String patientAddressState) {
    this.patientAddressState = patientAddressState;
  }

  public String getPatientAddressZip() {
    return patientAddressZip;
  }

  public void setPatientAddressZip(String patientAddressZip) {
    this.patientAddressZip = patientAddressZip;
  }

  public String getPatientAddressCountry() {
    return patientAddressCountry;
  }

  public void setPatientAddressCountry(String patientAddressCountry) {
    this.patientAddressCountry = patientAddressCountry;
  }

  public String getPatientAddressCountyParish() {
    return patientAddressCountyParish;
  }

  public void setPatientAddressCountyParish(String patientAddressCountyParish) {
    this.patientAddressCountyParish = patientAddressCountyParish;
  }

  public String getPatientPhone() {
    return patientPhone;
  }

  public void setPatientPhone(String patientPhone) {
    this.patientPhone = patientPhone;
  }

  public String getPatientEmail() {
    return patientEmail;
  }

  public void setPatientEmail(String patientEmail) {
    this.patientEmail = patientEmail;
  }

  public String getPatientEthnicity() {
    return patientEthnicity;
  }

  public void setPatientEthnicity(String patientEthnicity) {
    this.patientEthnicity = patientEthnicity;
  }

  public String getPatientBirthFlag() {
    return patientBirthFlag;
  }

  public void setPatientBirthFlag(String patientBirthFlag) {
    this.patientBirthFlag = patientBirthFlag;
  }

  public String getPatientBirthOrder() {
    return patientBirthOrder;
  }

  public void setPatientBirthOrder(String patientBirthOrder) {
    this.patientBirthOrder = patientBirthOrder;
  }

  public String getPatientDeathFlag() {
    return patientDeathFlag;
  }

  public void setPatientDeathFlag(String patientDeathFlag) {
    this.patientDeathFlag = patientDeathFlag;
  }

  public Date getPatientDeathDate() {
    return patientDeathDate;
  }

  public void setPatientDeathDate(Date patientDeathDate) {
    this.patientDeathDate = patientDeathDate;
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

  public int getPatientReportedId() {
    return patientReportedId;
  }

  public void setPatientReportedId(int reportedPatientId) {
    this.patientReportedId = reportedPatientId;
  }

  public String getPatientReportedExternalLink() {
    return patientReportedExternalLink;
  }

  public void setPatientReportedExternalLink(String reportedMrn) {
    this.patientReportedExternalLink = reportedMrn;
  }

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

  public String getPatientRace2() {
    return patientRace2;
  }

  public void setPatientRace2(String patientRace2) {
    this.patientRace2 = patientRace2;
  }

  public String getPatientRace3() {
    return patientRace3;
  }

  public void setPatientRace3(String patientRace3) {
    this.patientRace3 = patientRace3;
  }

  public String getPatientRace4() {
    return patientRace4;
  }

  public void setPatientRace4(String patientRace4) {
    this.patientRace4 = patientRace4;
  }

  public String getPatientRace5() {
    return patientRace5;
  }

  public void setPatientRace5(String patientRace5) {
    this.patientRace5 = patientRace5;
  }

  public String getPatientRace6() {
    return patientRace6;
  }

  public void setPatientRace6(String patientRace6) {
    this.patientRace6 = patientRace6;
  }

  @Override
  public String toString() {
    return "PatientReported{" + "patientReportedId=" + patientReportedId + ", orgReported="
        + orgReported + ", patientReportedExternalLink='" + patientReportedExternalLink + '\''
        + ", patient=" + patient + ", reportedDate=" + reportedDate + ", updatedDate=" + updatedDate
        + ", patientReportedAuthority='" + patientReportedAuthority + '\''
        + ", patientReportedType='" + patientReportedType + '\'' + ", patientNameLast='"
        + patientNameLast + '\'' + ", patientNameFirst='" + patientNameFirst + '\''
        + ", patientNameMiddle='" + patientNameMiddle + '\'' + ", patientMotherMaiden='"
        + patientMotherMaiden + '\'' + ", patientBirthDate=" + patientBirthDate + ", patientSex='"
        + patientSex + '\'' + ", patientRace='" + patientRace + '\'' + ", patientRace2='"
        + patientRace2 + '\'' + ", patientRace3='" + patientRace3 + '\'' + ", patientRace4='"
        + patientRace4 + '\'' + ", patientRace5='" + patientRace5 + '\'' + ", patientRace6='"
        + patientRace6 + '\'' + ", patientAddressLine1='" + patientAddressLine1 + '\''
        + ", patientAddressLine2='" + patientAddressLine2 + '\'' + ", patientAddressCity='"
        + patientAddressCity + '\'' + ", patientAddressState='" + patientAddressState + '\''
        + ", patientAddressZip='" + patientAddressZip + '\'' + ", patientAddressCountry='"
        + patientAddressCountry + '\'' + ", patientAddressCountyParish='"
        + patientAddressCountyParish + '\'' + ", patientPhone='" + patientPhone + '\''
        + ", patientEmail='" + patientEmail + '\'' + ", patientEthnicity='" + patientEthnicity
        + '\'' + ", patientBirthFlag='" + patientBirthFlag + '\'' + ", patientBirthOrder='"
        + patientBirthOrder + '\'' + ", patientDeathFlag='" + patientDeathFlag + '\''
        + ", patientDeathDate=" + patientDeathDate + ", publicityIndicator='" + publicityIndicator
        + '\'' + ", publicityIndicatorDate=" + publicityIndicatorDate + ", protectionIndicator='"
        + protectionIndicator + '\'' + ", protectionIndicatorDate=" + protectionIndicatorDate
        + ", registryStatusIndicator='" + registryStatusIndicator + '\''
        + ", registryStatusIndicatorDate=" + registryStatusIndicatorDate + ", guardianLast='"
        + guardianLast + '\'' + ", guardianFirst='" + guardianFirst + '\'' + ", guardianMiddle='"
        + guardianMiddle + '\'' + ", guardianRelationship='" + guardianRelationship + '\'' + '}';
  }
}
