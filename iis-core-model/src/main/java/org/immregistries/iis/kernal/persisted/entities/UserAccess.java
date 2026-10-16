package org.immregistries.iis.kernal.persisted.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.immregistries.iis.kernal.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Eric on 12/20/17.
 */

/**
 * This entity doubles as the Spring Security {@link Authentication} object: it is placed
 * directly in the SecurityContext and in the HttpSession by UserAccessUtil, and is therefore
 * read back <em>detached</em> on every subsequent request.
 * <p>
 * Two consequences drive the annotations below:
 * <ul>
 *   <li>{@link #roles} must be EAGER. {@link #getAuthorities()} is called from inside
 *   Spring's AuthorizationFilter, long after the loading transaction closed; a lazy
 *   collection would throw LazyInitializationException there.</li>
 *   <li>Every field must be {@code @JsonIgnore}d. AuthenticationRestController passes the
 *   principal into AuthInfo as a plain Object, so Jackson serializes this entity's runtime
 *   type on the publicly readable /rest/authentication endpoint.</li>
 * </ul>
 * New fields are nullable so that a database created from the pre-existing schema keeps
 * working: a NULL {@link #enabled} means enabled, and an empty {@link #roles} means
 * {@link UserRole#USER}.
 */
@Entity
@Table
public class UserAccess implements Serializable, Authentication {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id = 0;

	@Column(unique = true, nullable = false)
	private String accessName = "";

	@Column(nullable = false)
	@JsonIgnore
	private String accessKey = "";

	@JsonIgnore
	private String email;

	/**
	 * Wrapper type on purpose: rows predating this column read back as NULL, which means
	 * enabled. See {@link #isEnabled()}.
	 */
	@JsonIgnore
	private Boolean enabled;

	/**
	 * EAGER is mandatory, see the class javadoc.
	 * <p>
	 * The explicit VARCHAR type code is mandatory too. HAPI's dialects override
	 * {@code getEnumTypeDeclaration} to return null (true of HapiFhirH2Dialect and
	 * HapiFhirMySQLDialect alike), and Hibernate 6 writes that null straight into the
	 * generated DDL as the column type, producing {@code role null not null}. Naming the
	 * JDBC type keeps schema generation off that path on every dialect.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "UserAccess_roles", joinColumns = @JoinColumn(name = "userAccess_id"))
	@Column(name = "role", nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@JsonIgnore
	private Set<UserRole> roles = new HashSet<>();

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

	@JsonIgnore
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return false only when explicitly disabled; NULL (legacy rows) counts as enabled.
	 */
	@Transient
	@JsonIgnore
	public boolean isEnabled() {
		return !Boolean.FALSE.equals(enabled);
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@JsonIgnore
	public Set<UserRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<UserRole> roles) {
		this.roles = roles == null ? new HashSet<>() : roles;
	}

	@Transient
	@JsonIgnore
	public boolean hasRole(UserRole role) {
		return roles != null && roles.contains(role);
	}

	/**
	 * Users with no role recorded — which is every user predating the roles table — are
	 * treated as {@link UserRole#USER}.
	 */
	@Transient
	@JsonIgnore
	public Set<UserRole> getEffectiveRoles() {
		return roles == null || roles.isEmpty() ? Set.of(UserRole.USER) : Set.copyOf(roles);
	}

	@Override
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return getEffectiveRoles().stream()
			.map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getAuthority()))
			.collect(Collectors.toList());
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

	/**
	 * Deliberately always true. Returning the enabled flag here would make
	 * ExceptionTranslationFilter treat the caller as anonymous, clearing their session and
	 * redirecting to the login page instead of returning 401/403. Disabled accounts are
	 * rejected at authentication time in UserAccessUtil and re-checked against the database
	 * in UserAccessManagementService.
	 */
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

	@Override
	public int hashCode() {
		return this.getUserAccessId();
	}

	/**
	 * instanceof rather than getClass() so that a Hibernate proxy of a @ManyToOne
	 * userAccess still compares equal to the loaded entity.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserAccess)) {
			return false;
		}
		return ((UserAccess) obj).getUserAccessId() == this.getUserAccessId();
	}

	/**
	 * Never include accessKey or email: IisOAuthSuccessHandler logs the whole Authentication.
	 */
	@Override
	public String toString() {
		return "UserAccess{" +
			"id=" + id +
			", accessName='" + accessName + '\'' +
				'}';
	}
}
