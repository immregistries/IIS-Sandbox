package org.immregistries.iis.kernal.model;

import java.io.Serializable;

public class ModelPerson extends AbstractMappedObject implements Serializable {
  private static final long serialVersionUID = 1L;
	
  private String personId = "";
  private String personExternalLink = "";
  private Tenant tenant = null;
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

  public String getPersonId() {
    return personId;
  }

  public void setPersonId(String personId) {
    this.personId = personId;
  }

  public String getPersonExternalLink() {
    return personExternalLink;
  }

  public void setPersonExternalLink(String personExternalLink) {
    this.personExternalLink = personExternalLink;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
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
