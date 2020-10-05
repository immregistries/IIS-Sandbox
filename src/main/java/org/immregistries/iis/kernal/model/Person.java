package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class Person implements Serializable {
  private int personId = 0;
  private String personExternalLink = "";
  private OrgMaster orgMaster = null;
  private String nameLast = "";
  private String nameFirst = "";
  private String nameMiddle = "";
  private String assigningAuthority = "";
  private String nameTypeCode = "";
  private String identifierTypeCode = "";
  private String professionalSuffix = "";
  
  public String getProfessionalSuffix() {
    return professionalSuffix;
  }
  public void setProfessionalSuffix(String professionalSuffix) {
    this.professionalSuffix = professionalSuffix;
  }
  public int getPersonId() {
    return personId;
  }
  public void setPersonId(int personId) {
    this.personId = personId;
  }
  public String getPersonExternalLink() {
    return personExternalLink;
  }
  public void setPersonExternalLink(String personExternalLink) {
    this.personExternalLink = personExternalLink;
  }
  public OrgMaster getOrgMaster() {
    return orgMaster;
  }
  public void setOrgMaster(OrgMaster orgMaster) {
    this.orgMaster = orgMaster;
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
  public String getAssigningAuthority() {
    return assigningAuthority;
  }
  public void setAssigningAuthority(String assigningAuthority) {
    this.assigningAuthority = assigningAuthority;
  }
  public String getNameTypeCode() {
    return nameTypeCode;
  }
  public void setNameTypeCode(String nameTypeCode) {
    this.nameTypeCode = nameTypeCode;
  }
  public String getIdentifierTypeCode() {
    return identifierTypeCode;
  }
  public void setIdentifierTypeCode(String identifierTypeCode) {
    this.identifierTypeCode = identifierTypeCode;
  }

}
