package org.immregistries.iis.kernal.model.persisted;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ShLinkManifest {
	@JsonIgnore
	private Tenant tenant;
	@JsonIgnore
	private String id;
	@JsonProperty(value = "status")
	private String status; //"finalized"|"can-change"|"no-longer-valid"
	@JsonProperty(value = "files")
	private List<FileManifest> files = new ArrayList<>();

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

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class FileManifest {
		@JsonProperty(value = "contentType", required = true)
		private String contentType;
		@JsonProperty(value = "location")
		private String location;
		@JsonProperty(value = "embedded")
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