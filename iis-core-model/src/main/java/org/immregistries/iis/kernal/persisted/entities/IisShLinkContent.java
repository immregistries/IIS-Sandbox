package org.immregistries.iis.kernal.persisted.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Persisting generated key for smart health links and cards
 */
@Entity
@Table
public class IisShLinkContent {

	@Id
	@GeneratedValue
	private int id;

	@Column(columnDefinition = "TEXT")
	private String content;
	private Long exp;

	@JsonIgnore
	/**
	 * For referencing when stored, to be accessible in the UI
	 */
	private String patientId;

	@JsonIgnore
	/**
	 * For referencing when stored, to be accessible in the UI
	 */
	@ManyToOne
	private Tenant tenant;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getExp() {
		return exp;
	}

	public void setExp(Long exp) {
		this.exp = exp;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}
}
