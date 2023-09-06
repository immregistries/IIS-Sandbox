package org.immregistries.iis.kernal.model;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Eric on 12/20/17.
 */

/**
 * TODO Improve integration with spring security, especially grantedAuthority
 */
public class UserAccess implements Serializable, Authentication {

  private static final long serialVersionUID = 1L;

  private int userAccessId = 0;
  private String accessName = "";
  private String accessKey = "";


  public int getUserAccessId() {
    return userAccessId;
  }

  public void setUserAccessId(int userAccessId) {
    this.userAccessId = userAccessId;
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
