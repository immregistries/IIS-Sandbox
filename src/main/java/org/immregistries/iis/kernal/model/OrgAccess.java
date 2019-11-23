package org.immregistries.iis.kernal.model;

import java.io.Serializable;

/**
 * Created by Eric on 12/20/17.
 */
public class OrgAccess implements Serializable {
  private int orgAccessId = 0;
  private OrgMaster org = null;
  private String accessName = "";
  private String accessKey = "";

  public int getOrgAccessId() {
    return orgAccessId;
  }

  public void setOrgAccessId(int orgAccessId) {
    this.orgAccessId = orgAccessId;
  }

  public OrgMaster getOrg() {
    return org;
  }

  public void setOrg(OrgMaster org) {
    this.org = org;
  }

  public String getAccessName() {
    return accessName;
  }

  public void setAccessName(String accessName) {
    this.accessName = accessName;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }


}
