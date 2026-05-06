package org.immregistries.iis.kernal.persisted.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Entity
@Table
public class ShLinkManifest {
	@JsonIgnore
	@ManyToOne
	private Tenant tenant;

	@Id
	@Column(name = "id")
	private String id;

	@JsonProperty(value = "status")
	private String status; // "finalized"|"can-change"|"no-longer-valid"

	@JsonProperty(value = "files")
	@ElementCollection(fetch = FetchType.EAGER)
	private List<FileManifest> files = new ArrayList<>();

	@JsonIgnore
	private Boolean passwordProtected = false;

	@JsonIgnore
	private String passcode = null;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<FileManifest> getFiles() {
		return files;
	}

	public void setFiles(List<FileManifest> files) {
		this.files = files;
	}

	public void addFiles(FileManifest file) {
		if (this.files == null) {
			this.files = new ArrayList<>();
		}
		this.files.add(file);
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Boolean getPasswordProtected() {
		return passwordProtected;
	}

	public void setPasswordProtected(Boolean passwordProtected) {
		this.passwordProtected = passwordProtected;
	}

	public String getPasscode() {
		return passcode;
	}

	public void setPasscode(String passcode) {
		this.passcode = passcode;
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Embeddable
	public static class FileManifest {
		@JsonProperty(value = "contentType", required = true)
		private String contentType;
		@JsonProperty(value = "location")
		private String location;
		@JsonProperty(value = "embedded")
		@Column(columnDefinition = "TEXT")
		private String embedded;
		@JsonProperty(value = "lastUpdated")
		private Date lastUpdated;

		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getEmbedded() {
			return embedded;
		}

		public void setEmbedded(String embedded) {
			this.embedded = embedded;
		}

		public Date getLastUpdated() {
			return lastUpdated;
		}

		public void setLastUpdated(Date lastUpdated) {
			this.lastUpdated = lastUpdated;
		}
	}
}