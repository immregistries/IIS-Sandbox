package org.immregistries.iis.kernal.persisted.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Entity
@Table
public class ShLinkGenerated {

	@Id
	String id;

	@Column(columnDefinition = "TEXT")
	String url;

	String patientId;
	@ManyToOne
	Tenant tenant;
	Date createdAt;
	String flag; //SHLink flags (U/P/L)
	Long exp;// expiration epoch
	@Column(columnDefinition = "TEXT")
	String encodedQR;
	String label;
	String description;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public Long getExp() {
		return exp;
	}

	public void setExp(Long exp) {
		this.exp = exp;
	}

	public String getEncodedQR() {
		return encodedQR;
	}

	public void setEncodedQR(String encodedQR) {
		this.encodedQR = encodedQR;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
