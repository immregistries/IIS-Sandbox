package org.immregistries.iis.kernal.persisted.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
@Entity
@Table
public class UserAccess implements Serializable, Authentication {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id = 0;

	@Column(unique = true, nullable = false)
	private String accessName = "";

	@Column(nullable = false)
	@JsonIgnore
	private String accessKey = "";

	public int getUserAccessId() {
		return id;
	}

	public void setUserAccessId(int userAccessId) {
		this.id = userAccessId;
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
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return new ArrayList<>(0);
	}

	@Override
	@JsonIgnore
	public Object getCredentials() {
		return accessKey;
	}

	@Override
	@JsonIgnore
	public Object getDetails() {
		return this.getAccessName();
	}

	@Override
	@JsonIgnore
	public Object getPrincipal() {
		return this;
	}

	@Override
	@JsonIgnore
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
