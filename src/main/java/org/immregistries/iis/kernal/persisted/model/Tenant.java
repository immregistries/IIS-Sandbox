package org.immregistries.iis.kernal.persisted.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.immregistries.iis.kernal.model.ProcessingFlavor;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table
public class Tenant extends AbstractMappedObject implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private int orgId = 0;

	@JsonIgnore
	@ManyToOne
	private UserAccess userAccess = null;

	@Column(unique = true, nullable = false)
	private String organizationName = "";

	@JsonIgnore
	@Transient
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

	@Override
	public String toString() {
		return "Tenant{" +
				"orgId=" + orgId +
				", organizationName='" + organizationName + '\'' +
				'}';
	}
}
