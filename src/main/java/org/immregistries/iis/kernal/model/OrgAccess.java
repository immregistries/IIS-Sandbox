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
public class OrgAccess implements Serializable, Authentication
{

  private static final long serialVersionUID = 1L;

  private int orgAccessId = 0;
  private String accessName = "";
  private String accessKey = "";


  public int getOrgAccessId() {
    return orgAccessId;
  }

  public void setOrgAccessId(int orgAccessId) {
    this.orgAccessId = orgAccessId;
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
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return new ArrayList<>(0);
	}

	@Override
	public Object getCredentials() {
		return accessKey;
	}

	@Override
	public Object getDetails() {
		return this.getAccessName();
	}

	@Override
	public Object getPrincipal() {
		return this;
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
