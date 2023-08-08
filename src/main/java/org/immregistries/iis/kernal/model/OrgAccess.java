package org.immregistries.iis.kernal.model;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Eric on 12/20/17.
 */

/**
 * TODO IMPRove integration with spring security, especially grantedAuthority
 */
public class OrgAccess implements Serializable, Authentication, OAuth2User
{

  private static final long serialVersionUID = 1L;

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


	@Override
	public <A> A getAttribute(String name) {
		return OAuth2User.super.getAttribute(name);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return null;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return new ArrayList<>(0);
	}

	@Override
	public Object getCredentials() {
		return accessKey;
	}

	@Override
	public Object getDetails() {
		return org.getOrganizationName();
	}

	@Override
	public Object getPrincipal() {
		return org;
	}

	@Override
	public boolean isAuthenticated() {
		return true;
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

	}

	@Override
	public String getName() {
		return this.accessName;
	}
}
