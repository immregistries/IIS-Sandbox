package org.immregistries.iis.kernal.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Eric on 12/20/17.
 */
public class PatientMaster implements Serializable {
	
  private static final long serialVersionUID = 1L;

  private int patientId = 0;
  private OrgMaster orgMaster = null;
  private String patientExternalLink = "";
  private String patientNameLast = "";
  private String patientNameFirst = "";
  private String patientNameMiddle = "";
  private Date patientBirthDate = null;
  private String patientPhoneFrag = "";
  private String patientAddressFrag = "";
  private String patientSoundexLast = "";
  private String patientSoundexFirst = "";



  public int getPatientId() {
    return patientId;
  }

  public void setPatientId(int patientId) {
    this.patientId = patientId;
  }

  public String getPatientExternalLink() {
    return patientExternalLink;
  }

  public void setPatientExternalLink(String patientRegistryId) {
    this.patientExternalLink = patientRegistryId;
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

  public Date getPatientBirthDate() {
    return patientBirthDate;
  }

  public void setPatientBirthDate(Date patientBirthDate) {
    this.patientBirthDate = patientBirthDate;
  }

  public String getPatientPhoneFrag() {
    return patientPhoneFrag;
  }

  public void setPatientPhoneFrag(String patientPhoneFrag) {
    this.patientPhoneFrag = patientPhoneFrag;
  }

  public String getPatientAddressFrag() {
    return patientAddressFrag;
  }

  public void setPatientAddressFrag(String patientAddressFrag) {
    this.patientAddressFrag = patientAddressFrag;
  }

  public String getPatientSoundexLast() {
    return patientSoundexLast;
  }

  public void setPatientSoundexLast(String patientSoundexLast) {
    this.patientSoundexLast = patientSoundexLast;
  }

  public String getPatientSoundexFirst() {
    return patientSoundexFirst;
  }

  public void setPatientSoundexFirst(String patientSoundexFirst) {
    this.patientSoundexFirst = patientSoundexFirst;
  }

  public OrgMaster getOrgMaster() {
    return orgMaster;
  }

  public void setOrgMaster(OrgMaster orgMaster) {
    this.orgMaster = orgMaster;
  }


}
