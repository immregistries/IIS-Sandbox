package org.immregistries.iis.kernal.model.persisted;

import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.immregistries.iis.kernal.model.ProcessingFlavor;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Set;

public class Tenant extends AbstractMappedObject implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private int orgId = 0;
  @JsonIgnore
  private UserAccess userAccess = null;
  private String organizationName = "";
  @JsonIgnore
  private Set<ProcessingFlavor> processingFlavorSet = null;

  public int getOrgId() {
    return orgId;
  }

  public void setOrgId(int orgId) {
    this.orgId = orgId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  @Override
  public int hashCode() {
    return this.getOrgId();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Tenant) {
      Tenant other = (Tenant) obj;
      return other.getOrgId() == this.getOrgId();
    }
    return super.equals(obj);
  }

  public Set<ProcessingFlavor> getProcessingFlavorSet() {
    if (processingFlavorSet == null) {
      processingFlavorSet = ProcessingFlavor.getProcessingStyle(organizationName);
    }
    return processingFlavorSet;
  }

  public UserAccess getUserAccess() {
    return userAccess;
  }

  public void setUserAccess(UserAccess userAccess) {
    this.userAccess = userAccess;
  }

  @Override
  public String toString() {
    return "Tenant{" +
        "orgId=" + orgId +
        ", organizationName='" + organizationName + '\'' +
        '}';
  }
}
